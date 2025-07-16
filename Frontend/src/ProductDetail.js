import React, { useState, useEffect } from 'react';
import { useNavigate, useLocation } from 'react-router-dom';
import apiClient from './api/AxiosConfig'; // Import the configured Axios instance
import './ProductDetail.css'; // Make sure to create this CSS file

function ProductDetail({ onAddToCart }) {
    const navigate = useNavigate();
    const location = useLocation();
    const productId = location.pathname.split('/').pop();

    const [product, setProduct] = useState(null);
    const [loading, setLoading] = useState(true);
    const [error, setError] = useState('');
    const [quantity, setQuantity] = useState(1);
    const [addingToCart, setAddingToCart] = useState(false);
    const [similarProducts, setSimilarProducts] = useState([]);
    const [loadingSimilar, setLoadingSimilar] = useState(false);

    useEffect(() => {
        const fetchProductDetail = async () => {
            try {
                setLoading(true);
                setError('');
                // Use apiClient instead of fetch
                const response = await apiClient.get(`/api/products/${productId}`);
                if (response.status === 200) {
                    setProduct(response.data);
                } else {
                    setError(`Không thể tải thông tin sản phẩm: ${response.statusText}`);
                }
            } catch (err) {
                if (err.response) {
                    setError(`Lỗi từ máy chủ: ${err.response.data.message || err.response.statusText}`);
                } else if (err.request) {
                    setError(`Không có phản hồi từ máy chủ. Vui lòng kiểm tra kết nối mạng.`);
                } else {
                    setError(`Lỗi kết nối: ${err.message}`);
                }
            } finally {
                setLoading(false);
            }
        };

        const fetchSimilarProducts = async () => {
            try {
                setLoadingSimilar(true);
                const response = await apiClient.get(`/api/recommendations/similar/${productId}`);
                if (response.status === 200 && response.data.success) {
                    setSimilarProducts(response.data.data || []);
                }
            } catch (err) {
                console.error('Error fetching similar products:', err);
            } finally {
                setLoadingSimilar(false);
            }
        };

        if (productId) {
            fetchProductDetail();
            fetchSimilarProducts();
        }
    }, [productId]);

    const handleQuantityChange = (e) => {
        const value = parseInt(e.target.value);
        if (!isNaN(value) && value > 0) {
            setQuantity(Math.min(value, product?.stock || 1));
        }
    };

    const handleAddToCartClick = async () => {
        if (!product || product.stock <= 0) {
            alert('Sản phẩm này hiện không có sẵn hoặc hết hàng.');
            return;
        }

        try {
            setAddingToCart(true);
            const success = await onAddToCart(product._id, quantity);
            if (success) {
                alert(`Đã thêm ${quantity} sản phẩm ${product.name} vào giỏ hàng!`);
                // Refresh product stock using apiClient
                const response = await apiClient.get(`/api/products/${productId}`);
                if (response.status === 200) {
                    const updatedProduct = response.data;
                    setProduct(updatedProduct);
                }
            }
        } catch (err) {
            alert('Có lỗi xảy ra khi thêm vào giỏ hàng. Vui lòng thử lại.');
        } finally {
            setAddingToCart(false);
        }
    };

    const handleBuyNowClick = () => {
        if (!product || product.stock <= 0) {
            alert('Sản phẩm này hiện không có sẵn hoặc hết hàng.');
            return;
        }

        const selectedProductIds = [product._id];
        // For "Buy Now", we only have one product, and no vouchers are pre-selected
        // We will pass the quantity of the product here
        const productForCheckout = {
            ...product,
            quantity: quantity,
        };

        navigate('/checkout', {
            state: {
                selectedProductIds: [productForCheckout._id],
                productsFromBuyNow: [productForCheckout], // Pass the product with quantity directly
                selectedVouchers: {},
                isBuyNow: true // Add this flag for Buy Now flow
            }
        });
    };

    const handleBackClick = () => {
        navigate(-1); // Go back to the previous page in history
    };

    const refreshProductData = async () => {
        try {
            const response = await apiClient.get(`/api/products/${productId}`);
            if (response.status === 200) {
                const updatedProduct = response.data;
                setProduct(updatedProduct);
                console.log('Product data refreshed after stock change');
            }
        } catch (error) {
            console.error('Error refreshing product data:', error);
        }
    };

    // Listen for order completion to refresh product data
    useEffect(() => {
        const handleOrderCompletion = (event) => {
            if (event.detail && event.detail.productIds && event.detail.productIds.includes(productId)) {
                refreshProductData();
            }
        };
        
        window.addEventListener('orderCompleted', handleOrderCompletion);
        
        return () => {
            window.removeEventListener('orderCompleted', handleOrderCompletion);
        };
    }, [productId]);

    if (loading) {
        return (
            <div className="product-detail-container loading">
                <div className="loading-spinner"></div>
                <p>Đang tải chi tiết sản phẩm...</p>
            </div>
        );
    }

    if (error) {
        return (
            <div className="product-detail-container error">
                <p className="error-message">{error}</p>
                <button onClick={handleBackClick} className="back-button">← Quay lại</button>
            </div>
        );
    }

    if (!product) {
        return (
            <div className="product-detail-container not-found">
                <p>Không tìm thấy thông tin sản phẩm.</p>
                <button onClick={handleBackClick} className="back-button">← Quay lại</button>
            </div>
        );
    }

    return (
        <div className="product-detail-container">
            <button onClick={handleBackClick} className="back-button">← Quay lại</button>
            <div className="product-detail-content">
                <div
                    className="product-images"
                    style={product.image_url ? {
                        backgroundImage: `url(${product.image_url})`,
                        backgroundSize: 'contain',
                        backgroundRepeat: 'no-repeat',
                        backgroundPosition: 'center',
                    } : {}}
                >
                    {!product.image_url && <div className="no-image">Không có hình ảnh</div>}
                </div>
                <div className="product-info">
                    <h2 className="product-name">{product.name}</h2>
                    <p className="description"><strong>Mô tả:</strong> {product.description}</p>
                    <p className="price"><strong>Giá:</strong> {product.price.toLocaleString('vi-VN')} VND</p>
                    <p className="stock"><strong>Tồn kho:</strong> {product.stock}</p>
                    <p className="category"><strong>Danh mục:</strong> {product.category}</p>
                    <p className="shop-name"><strong>Cửa hàng:</strong> {product.shop_name}</p>

                    <div className="add-to-cart-section">
                        <label htmlFor="quantity">Số lượng:</label>
                        <input
                            type="number"
                            id="quantity"
                            value={quantity}
                            onChange={handleQuantityChange}
                            min="1"
                            max={product.stock}
                            disabled={product.stock <= 0}
                        />
                        <button
                            onClick={handleAddToCartClick}
                            disabled={product.stock <= 0 || quantity > product.stock || addingToCart}
                            className="add-to-cart-button"
                        >
                            {addingToCart ? 'Đang thêm...' : 'Thêm vào giỏ hàng'}
                        </button>
                        <button
                            onClick={handleBuyNowClick}
                            disabled={product.stock <= 0 || quantity > product.stock}
                            className="buy-now-button"
                        >
                            Mua Ngay
                        </button>
                    </div>
                </div>
            </div>

            {/* Similar Products Section */}
            <div className="similar-products-section">
                <h3>Sản phẩm tương tự</h3>
                {loadingSimilar ? (
                    <div className="loading-similar">
                        <div className="loading-spinner"></div>
                        <p>Đang tải sản phẩm tương tự...</p>
                    </div>
                ) : similarProducts.length > 0 ? (
                    <div className="similar-products-grid">
                        {similarProducts.map(p => (
                            <div key={p._id} className="product-item">
                                <div
                                    className="product-image-container"
                                    style={p.image_url ? { backgroundImage: `url(${p.image_url})` } : {}}
                                >
                                    {!p.image_url && <div className="no-image">Không có hình ảnh</div>}
                                    {p.stock === 0 && (
                                        <img src="/soldout.png" alt="Sold Out" className="sold-out-overlay" />
                                    )}
                                </div>
                                <h3>{p.name}</h3>
                                <div className="product-meta">
                                    <p className="stock">Tồn kho: {p.stock}</p>
                                    <p className="sold-count">Đã bán: {p.purchaseCount || 0}</p>
                                </div>
                                <p className="price">{p.price.toLocaleString('vi-VN')} VND</p>
                                <div className="actions">
                                    <button onClick={() => navigate(`/product/${p._id}`)} className="secondary-btn">Xem chi tiết</button>
                                </div>
                            </div>
                        ))}
                    </div>
                ) : (
                    <p>Không có sản phẩm tương tự.</p>
                )}
            </div>
        </div>
    );
}

export default ProductDetail;