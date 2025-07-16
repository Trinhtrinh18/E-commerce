import React, { useState, useEffect } from 'react';
import { useNavigate, useLocation } from 'react-router-dom';
import './ShoppingCart.css';

function Checkout() {
    const [cartProducts, setCartProducts] = useState([]);
    const [message, setMessage] = useState('');
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
    const [isPlacingOrder, setIsPlacingOrder] = useState(false);
    const navigate = useNavigate();
    const location = useLocation();

    useEffect(() => {
        if (location.state && location.state.productsFromBuyNow) {
            setCartProducts(location.state.productsFromBuyNow);
            // Also set selectedProductIds for a single product buy now
            setSelectedProductIds(location.state.productsFromBuyNow.map(p => p._id));
        } else {
            fetchCartProducts();
            // Re-add logic to set selectedProductIds when coming from the cart
            if (location.state && location.state.selectedProductIds) {
                setSelectedProductIds(location.state.selectedProductIds);
            }
        }
        fetchUserProfile();
        if (location.state && location.state.selectedVouchers) {
            setSelectedVouchers(location.state.selectedVouchers);
        }
    }, []);

    const fetchUserProfile = async () => {
        try {
            const token = localStorage.getItem('token');
            if (!token) return;
            const response = await fetch('http://localhost:8080/api/users/me', {
                headers: { 'Authorization': `Bearer ${token}` }
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
            setMessage('Lỗi lấy thông tin người dùng');
        }
    };

    const fetchCartProducts = async () => {
        try {
            const token = localStorage.getItem('token');
            if (!token) return;
            const response = await fetch('http://localhost:8080/api/customers/cart', {
                headers: { 'Authorization': `Bearer ${token}` }
            });
            if (response.ok) {
                const data = await response.json();
                setCartProducts(data);
            }
        } catch (error) {
            setMessage('Lỗi lấy giỏ hàng');
        }
    };

    const fetchVouchersForProduct = async (productId) => {
        try {
            const token = localStorage.getItem('token');
            const response = await fetch(`http://localhost:8080/api/vouchers/product/${productId}`, {
                headers: { 'Authorization': `Bearer ${token}` }
            });
            if (response.ok) {
                const vouchers = await response.json();
                setAvailableVouchers(prev => ({ ...prev, [productId]: vouchers }));
            }
        } catch { }
    };

    useEffect(() => {
        cartProducts.forEach(product => {
            fetchVouchersForProduct(product._id);
        });
    }, [cartProducts]);

    const handleVoucherSelect = (productId, voucherId) => {
        setSelectedVouchers(prev => ({ ...prev, [productId]: voucherId }));
    };

    const calculateItemTotal = (item) => {
        const price = parseFloat(item.price);
        const quantity = parseInt(item.quantity);
        let total = price * quantity;
        const selectedVoucher = selectedVouchers[item._id];
        const productVouchers = availableVouchers[item._id] || [];
        const voucher = productVouchers.find(v => v.id === selectedVoucher);
        if (voucher) {
            if (voucher.discountType === 'PERCENTAGE') {
                total = total * (1 - voucher.discountValue / 100);
            } else if (voucher.discountType === 'FIXED') {
                total = total - voucher.discountValue;
            }
        }
        return total;
    };

    const displayedProducts = selectedProductIds.length > 0
        ? cartProducts.filter(p => selectedProductIds.includes(p._id))
        : [];

    const calculateTotal = () => {
        return displayedProducts.reduce((sum, item) => {
            if (!selectedProductIds.includes(item._id)) return sum;
            return sum + calculateItemTotal(item);
        }, 0);
    };

    const handlePlaceOrder = async (e) => {
        e.preventDefault();
        
        // Ngăn chặn nhấn nút nhiều lần
        if (isPlacingOrder) {
            return;
        }
        
        if (!shippingAddress || !paymentMethod || !fullName || !phoneNumber) {
            setMessage('Vui lòng điền đầy đủ thông tin giao hàng.');
            return;
        }
        if (paymentMethod === 'Thẻ ngân hàng' && !bankAccount) {
            setMessage('Vui lòng cập nhật thông tin tài khoản ngân hàng trong hồ sơ.');
            return;
        }
        if (displayedProducts.length === 0) {
            setMessage('Vui lòng chọn ít nhất một sản phẩm để đặt hàng.');
            return;
        }
        
        setIsPlacingOrder(true);
        setMessage('Đang xử lý đơn hàng...');
        
        const orderItems = displayedProducts.map(item => ({
            productId: item._id,
            quantity: parseInt(item.quantity),
            voucherId: selectedVouchers[item._id]
        }));
        
        try {
            const token = localStorage.getItem('token');
            const response = await fetch('http://localhost:8080/api/customers/orders', {
                method: 'POST',
                headers: {
                    'Content-Type': 'application/json',
                    'Authorization': `Bearer ${token}`
                },
                body: JSON.stringify({
                    fullName,
                    phoneNumber,
                    shippingAddress,
                    paymentMethod,
                    bankAccount: paymentMethod === 'Thẻ ngân hàng' ? bankAccount : null,
                    items: orderItems,
                    isBuyNow: location.state?.isBuyNow || false // Pass the isBuyNow flag to backend
                })
            });
            if (response.ok) {
                setMessage('Đặt hàng thành công!');
                
                // Dispatch event để thông báo order completion
                const productIds = displayedProducts.map(p => p._id);
                const orderCompletionEvent = new CustomEvent('orderCompleted', {
                    detail: { productIds }
                });
                window.dispatchEvent(orderCompletionEvent);
                
                // Làm mới giỏ hàng nếu không phải Buy Now
                if (!location.state?.isBuyNow) {
                    // Làm mới giỏ hàng từ server
                    fetchCartProducts();
                }
                
                setTimeout(() => navigate('/'), 2000);
            } else {
                // Attempt to parse error message from backend
                const errorData = await response.json();
                setMessage(errorData.message || 'Lỗi tạo đơn hàng!');
            }
        } catch (error) {
            // Handle network errors or other unexpected errors
            setMessage('Lỗi mạng khi tạo đơn hàng!');
        } finally {
            setIsPlacingOrder(false);
        }
    };

    return (
        <div className="shopping-cart-container">
            <button onClick={() => navigate(-1)} className="back-btn">
                <svg width="18" height="18" viewBox="0 0 18 18" fill="none" xmlns="http://www.w3.org/2000/svg" style={{ marginRight: 4 }}>
                    <path d="M11.25 14.25L6.75 9L11.25 3.75" stroke="#ee4d2d" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round" />
                </svg>
                Quay lại
            </button>
            <h2>Thanh toán đơn hàng</h2>
            {message && <p className="message error">{message}</p>}
            {displayedProducts.length > 0 ? (
                <div className="cart-list">
                    {displayedProducts.map(product => (
                        <div key={product._id} className="cart-item">
                            <div className="product-thumbnail"
                                style={product.image_url ? {
                                    backgroundImage: `url(${product.image_url})`,
                                    backgroundSize: 'cover',
                                    backgroundRepeat: 'no-repeat',
                                    backgroundPosition: 'center',
                                } : {}}>
                                {!product.image_url && <div className="no-image-thumbnail"></div>}
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
                                                {(availableVouchers[product._id] || []).map(voucher => (
                                                    <option key={voucher.id} value={voucher.id}>
                                                        {voucher.discountType === 'PERCENTAGE'
                                                            ? `Giảm ${voucher.discountValue}%`
                                                            : `Giảm ${voucher.discountValue.toLocaleString('vi-VN')} VND`}
                                                    </option>
                                                ))}
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
                                <span>{product.quantity}</span>
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
                        </div>
                    ))}
                    <h3 className="cart-total">Tổng cộng: {calculateTotal().toLocaleString('vi-VN')} VND</h3>
                    <form onSubmit={handlePlaceOrder} className="order-form">
                        <h4>Thông tin đặt hàng</h4>
                        <div className="form-group">
                            <label htmlFor="fullName">Họ và tên người nhận</label>
                            <input
                                type="text"
                                id="fullName"
                                value={fullName}
                                onChange={e => setFullName(e.target.value)}
                                required
                            />
                        </div>
                        <div className="form-group">
                            <label htmlFor="phoneNumber">Số điện thoại</label>
                            <input
                                type="tel"
                                id="phoneNumber"
                                value={phoneNumber}
                                onChange={e => setPhoneNumber(e.target.value)}
                                required
                            />
                        </div>
                        <div className="form-group">
                            <label htmlFor="shippingAddress">Địa chỉ giao hàng</label>
                            {userAddresses.length > 0 && (
                                <select
                                    value={selectedAddressIndex}
                                    onChange={(e) => {
                                        const idx = parseInt(e.target.value);
                                        setSelectedAddressIndex(idx);
                                        if (idx === -1) {
                                            setShippingAddress('');
                                        } else {
                                            const addr = userAddresses[idx];
                                            setShippingAddress(`${addr.street}, ${addr.ward}, ${addr.district}, ${addr.city}`);
                                        }
                                    }}
                                    className="address-select"
                                >
                                    <option value={-1}>Địa chỉ khác</option>
                                    {userAddresses.map((addr, idx) => (
                                        <option key={idx} value={idx}>
                                            {`${addr.street}, ${addr.ward}, ${addr.district}, ${addr.city}`}
                                        </option>
                                    ))}
                                </select>
                            )}
                            <input
                                type="text"
                                id="shippingAddress"
                                value={shippingAddress}
                                onChange={e => {
                                    setShippingAddress(e.target.value);
                                    setSelectedAddressIndex(-1);
                                }}
                                required
                                placeholder={selectedAddressIndex === -1 ? "Nhập địa chỉ giao hàng" : ""}
                            />
                        </div>
                        <div className="form-group">
                            <label htmlFor="paymentMethod">Phương thức thanh toán</label>
                            <select
                                id="paymentMethod"
                                value={paymentMethod}
                                onChange={e => setPaymentMethod(e.target.value)}
                                required
                            >
                                <option value="Tiền mặt">Tiền mặt khi nhận hàng</option>
                                <option value="Thẻ ngân hàng">Thanh toán bằng thẻ ngân hàng</option>
                            </select>
                        </div>
                        {paymentMethod === 'Thẻ ngân hàng' && bankAccount && (
                            <div className="bank-info">
                                <h5>Thông tin tài khoản ngân hàng</h5>
                                <div className="bank-details">
                                    <p><strong>Ngân hàng:</strong> {bankAccount.bankName}</p>
                                    <p><strong>Số tài khoản:</strong> {bankAccount.accountNumber}</p>
                                    <p><strong>Chủ tài khoản:</strong> {bankAccount.accountHolder}</p>
                                </div>
                            </div>
                        )}
                        <button type="submit" className="place-order-btn" disabled={isPlacingOrder}>
                            {isPlacingOrder ? 'Đang xử lý...' : 'Thanh toán'}
                        </button>
                    </form>
                </div>
            ) : (
                <p>Không có sản phẩm nào trong giỏ hàng.</p>
            )}
        </div>
    );
}

export default Checkout; 