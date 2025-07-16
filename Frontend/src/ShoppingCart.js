import React, { useState, useEffect, useRef } from 'react';
import './ShoppingCart.css';
import { useNavigate } from 'react-router-dom';

function ShoppingCart({ onPlaceOrder }) {
    const [cartProducts, setCartProducts] = useState([]);
    const [message, setMessage] = useState('');
    const [messageTimeoutId, setMessageTimeoutId] = useState(null);
    const [shippingAddress, setShippingAddress] = useState('');
    const [paymentMethod, setPaymentMethod] = useState('Tiền mặt');
    const [userAddresses, setUserAddresses] = useState([]);
    const [selectedAddressIndex, setSelectedAddressIndex] = useState(-1);
    const [fullName, setFullName] = useState('');
    const [phoneNumber, setPhoneNumber] = useState('');
    const [bankAccount, setBankAccount] = useState(null);
    const [availableVouchers, setAvailableVouchers] = useState({});
    const [selectedVouchers, setSelectedVouchers] = useState({});
    const [selectedProductIds, setSelectedProductIds] = useState([]);
    const hasAutoSelected = useRef(false);
    const navigate = useNavigate();

    const displayMessage = (msg, duration = 3000) => {
        if (messageTimeoutId) {
            clearTimeout(messageTimeoutId);
        }
        setMessage(msg);
        const id = setTimeout(() => {
            setMessage('');
            setMessageTimeoutId(null);
        }, duration);
        setMessageTimeoutId(id);
    };

    const fetchUserProfile = async () => {
        try {
            const token = localStorage.getItem('token');
            if (!token) return;

            const response = await fetch('http://localhost:8080/api/users/me', {
                headers: {
                    'Authorization': `Bearer ${token}`
                }
            });

            if (response.ok) {
                const data = await response.json();
                setFullName(data.fullName || '');

                if (data.buyerProfile) {
                    setPhoneNumber(data.buyerProfile.phoneNumber || '');
                    setBankAccount(data.buyerProfile.bankAccount || null);

                    if (data.buyerProfile.primaryAddress) {
                        const primaryAddress = data.buyerProfile.primaryAddress;
                        const formattedAddress = `${primaryAddress.street}, ${primaryAddress.ward}, ${primaryAddress.district}, ${primaryAddress.city}`;
                        setShippingAddress(formattedAddress);
                    }

                    if (data.buyerProfile.addresses) {
                        setUserAddresses(data.buyerProfile.addresses);
                    }
                }
            }
        } catch (error) {
            console.error('Error fetching user profile:', error);
        }
    };

    const fetchCartProducts = async () => {
        try {
            const token = localStorage.getItem('token');
            if (!token) {
                displayMessage('Bạn cần đăng nhập để xem giỏ hàng.', 5000);
                return;
            }
            const response = await fetch('http://localhost:8080/api/customers/cart', {
                headers: {
                    'Authorization': `Bearer ${token}`
                }
            });

            if (response.ok) {
                const data = await response.json();
                setCartProducts(data);
            } else {
                const errorText = await response.text();
                displayMessage(`Error fetching cart: ${errorText}`, 5000);
            }
        } catch (error) {
            displayMessage(`Network error fetching cart: ${error.message}`, 5000);
        }
    };

    const fetchVouchersForProduct = async (productId) => {
        try {
            const token = localStorage.getItem('token');
            const response = await fetch(`http://localhost:8080/api/vouchers/product/${productId}`, {
                headers: {
                    'Authorization': `Bearer ${token}`
                }
            });
            if (response.ok) {
                const vouchers = await response.json();
                setAvailableVouchers(prev => ({
                    ...prev,
                    [productId]: vouchers
                }));
            } else {
                console.error(`Failed to fetch vouchers for product ${productId}:`, response.status, response.statusText);
            }
        } catch (error) {
            console.error('Error fetching vouchers:', error);
        }
    };

    const handleVoucherSelect = (productId, voucherId) => {
        setSelectedVouchers(prev => ({
            ...prev,
            [productId]: voucherId
        }));
    };

    useEffect(() => {
        fetchCartProducts();
        fetchUserProfile();
    }, []);

    useEffect(() => {
        cartProducts.forEach(product => {
            fetchVouchersForProduct(product._id);
        });
        setSelectedProductIds(prev => {
            // Filter out products that are now out of stock
            const newSelectedIds = prev.filter(id => {
                const productInCart = cartProducts.find(p => p._id === id);
                return productInCart && productInCart.stock > 0;
            });

            if (!hasAutoSelected.current && newSelectedIds.length === 0 && cartProducts.length > 0) {
                hasAutoSelected.current = true;
                // Only auto-select products that are in stock
                return cartProducts.filter(p => p.stock > 0).map(p => p._id);
            }
            return newSelectedIds;
        });
    }, [cartProducts]);

    const handleUpdateQuantity = async (productId, quantity) => {
        try {
            const token = localStorage.getItem('token');
            const response = await fetch(`http://localhost:8080/api/customers/cart/update-quantity/${productId}/${quantity}`, {
                method: 'PUT',
                headers: {
                    'Authorization': `Bearer ${token}`
                }
            });
            if (response.ok) {
                fetchCartProducts();
                displayMessage('Số lượng sản phẩm đã được cập nhật.');
            } else {
                const errorText = await response.text();
                displayMessage(`Error updating quantity: ${errorText}`, 5000);
            }
        } catch (error) {
            displayMessage(`Network error updating quantity: ${error.message}`, 5000);
        }
    };

    const handleRemoveProduct = async (productId) => {
        try {
            const token = localStorage.getItem('token');
            const response = await fetch('http://localhost:8080/api/customers/cart/remove', {
                method: 'DELETE',
                headers: {
                    'Content-Type': 'application/json',
                    'Authorization': `Bearer ${token}`
                },
                body: JSON.stringify({ productId })
            });
            if (response.ok) {
                fetchCartProducts();
                displayMessage('Sản phẩm đã được xóa khỏi giỏ hàng.');
            } else {
                const errorText = await response.text();
                displayMessage(`Error removing product: ${errorText}`, 5000);
            }
        } catch (error) {
            displayMessage(`Network error removing product: ${error.message}`, 5000);
        }
    };

    const calculateItemTotal = (item) => {
        const price = parseFloat(item.price);
        const quantity = parseInt(item.quantity);
        let total = price * quantity;

        // Apply voucher discount if selected
        const selectedVoucher = selectedVouchers[item._id];
        const productVouchers = availableVouchers[item._id] || [];
        const voucher = productVouchers.find(v => v.id === selectedVoucher);

        if (voucher) {
            // Check if minimum order value is met
            const canUseVoucher = !voucher.minOrderValue || total >= voucher.minOrderValue;

            if (canUseVoucher) {
                if (voucher.discountType === 'PERCENTAGE') {
                    total = total * (1 - voucher.discountValue / 100);
                } else if (voucher.discountType === 'FIXED') {
                    total = total - voucher.discountValue;
                }
            }
        }

        return total;
    };

    const calculateTotal = () => {
        return cartProducts.reduce((sum, item) => {
            if (!selectedProductIds.includes(item._id)) return sum;
            return sum + calculateItemTotal(item);
        }, 0);
    };

    const handleProductCheckbox = (productId) => {
        setSelectedProductIds(prev =>
            prev.includes(productId)
                ? prev.filter(id => id !== productId)
                : [...prev, productId]
        );
    };

    const handleSelectAll = (e) => {
        if (e.target.checked) {
            setSelectedProductIds(cartProducts.filter(p => p.stock > 0).map(p => p._id));
        } else {
            setSelectedProductIds([]);
        }
    };

    const handleGoToCheckout = () => {
        navigate('/checkout', { state: { selectedVouchers, selectedProductIds } });
    };

    return (
        <div className="shopping-cart-container">
            <button onClick={() => navigate(-1)} className="back-btn">
                <i className="fas fa-arrow-left"></i> Quay lại
            </button>
            <h2>Giỏ hàng của bạn</h2>
            {message && <p className="message error">{message}</p>}

            {cartProducts.length > 0 ? (
                <div className="cart-list">
                    <div className="cart-items-scroll">
                        <div style={{ display: 'flex', alignItems: 'center', marginBottom: 10 }}>
                            <input
                                type="checkbox"
                                checked={selectedProductIds.length === cartProducts.filter(p => p.stock > 0).length && cartProducts.filter(p => p.stock > 0).length > 0}
                                onChange={handleSelectAll}
                                style={{ marginRight: 8 }}
                            />
                            <span>Chọn tất cả</span>
                        </div>
                        {cartProducts.map(product => (
                            <div key={product._id} className="cart-item">
                                <input
                                    type="checkbox"
                                    checked={selectedProductIds.includes(product._id)}
                                    onChange={() => handleProductCheckbox(product._id)}
                                    disabled={product.stock === 0} /* Disable if stock is 0 */
                                />
                                <div
                                    className="product-thumbnail"
                                    style={product.image_url ? {
                                        backgroundImage: `url(${product.image_url})`,
                                        backgroundSize: 'cover',
                                        backgroundRepeat: 'no-repeat',
                                        backgroundPosition: 'center',
                                    } : {}}>
                                    {!product.image_url && <div className="no-image-thumbnail"></div>}
                                    {product.stock === 0 && (
                                        <img src="/soldout.png" alt="Sold Out" className="sold-out-overlay" />
                                    )}
                                </div>
                                <div className="cart-item-details">
                                    <h3>{product.name}</h3>
                                    <p className="product-shop-name">Cửa hàng: {product.shop_name}</p>
                                    <p className="price">{product.price.toLocaleString('vi-VN')} VND</p>
                                    <p className="stock">Tồn kho: {product.stock}</p>
                                    <div className="actions">
                                        <div className="voucher-section">
                                            {(availableVouchers[product._id]?.length > 0) ? (
                                                <select
                                                    value={selectedVouchers[product._id] || ''}
                                                    onChange={(e) => handleVoucherSelect(product._id, e.target.value)}
                                                    className="voucher-select"
                                                >
                                                    <option value="">Chọn voucher</option>
                                                    {(availableVouchers[product._id] || []).map(voucher => {
                                                        const canUse = !voucher.minOrderValue || (product.price * product.quantity) >= voucher.minOrderValue;
                                                        const displayText = voucher.discountType === 'PERCENTAGE'
                                                            ? `Giảm ${voucher.discountValue}%`
                                                            : `Giảm ${voucher.discountValue.toLocaleString('vi-VN')} VND`;
                                                        const minOrderText = voucher.minOrderValue
                                                            ? ` (Tối thiểu ${voucher.minOrderValue.toLocaleString('vi-VN')} VND)`
                                                            : '';

                                                        return (
                                                            <option
                                                                key={voucher.id}
                                                                value={voucher.id}
                                                                disabled={!canUse}
                                                            >
                                                                {displayText}{minOrderText}{!canUse ? ' - Không đủ điều kiện' : ''}
                                                            </option>
                                                        );
                                                    })}
                                                </select>
                                            ) : (
                                                <div className="no-voucher-message">
                                                    Không có voucher khả dụng
                                                </div>
                                            )}
                                        </div>
                                    </div>
                                </div>
                                <div className="quantity-control">
                                    <button onClick={() => handleUpdateQuantity(product._id, product.quantity - 1)}>-</button>
                                    <span>{product.quantity}</span>
                                    <button onClick={() => handleUpdateQuantity(product._id, product.quantity + 1)}>+</button>
                                </div>
                                <div className="price-info">
                                    {selectedVouchers[product._id] ? (
                                        <>
                                            <p className="original-price">
                                                {(product.price * product.quantity).toLocaleString('vi-VN')} VND
                                            </p>
                                            <p className="discounted-price">
                                                {calculateItemTotal(product).toLocaleString('vi-VN')} VND
                                            </p>
                                        </>
                                    ) : (
                                        <p className="normal-price">
                                            {(product.price * product.quantity).toLocaleString('vi-VN')} VND
                                        </p>
                                    )}
                                </div>
                                <button onClick={() => handleRemoveProduct(product._id)} className="remove-btn">Xóa</button>
                            </div>
                        ))}
                    </div>
                    <h3 className="cart-total">Tổng cộng: {calculateTotal().toLocaleString('vi-VN')} VND</h3>
                    <button className="place-order-btn" onClick={handleGoToCheckout} disabled={selectedProductIds.length === 0}>
                        Đặt hàng
                    </button>
                </div>
            ) : (
                <p>Giỏ hàng trống</p>
            )}
        </div>
    );
}

export default ShoppingCart;