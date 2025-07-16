  
import React, { useState, useEffect } from 'react';
import { useNavigate, useParams } from 'react-router-dom';
import axios from 'axios';
import './CustomerOrderDetail.css';

function CustomerOrderDetail() {
    const navigate = useNavigate();
    const { orderId } = useParams();
    const [order, setOrder] = useState(null);
    const [loading, setLoading] = useState(true);
    const [error, setError] = useState('');
    const [productCache, setProductCache] = useState({});

    const statusLabels = {
        'PENDING': 'Chờ xác nhận',
        'Pending': 'Chờ xác nhận',
        'pending': 'Chờ xác nhận',
        'CONFIRMED': 'Đã xác nhận',
        'SHIPPING': 'Đang giao hàng',
        'DELIVERED': 'Đã giao hàng',
        'CANCELLED': 'Đã hủy'
    };

    const statusColors = {
        'PENDING': '#f39c12',
        'Pending': '#f39c12',
        'pending': '#f39c12',
        'CONFIRMED': '#3498db',
        'SHIPPING': '#9b59b6',
        'DELIVERED': '#27ae60',
        'CANCELLED': '#e74c3c'
    };

    const statusSteps = ['PENDING', 'CONFIRMED', 'SHIPPING', 'DELIVERED'];

    const fetchOrderDetail = async () => {
        try {
            setLoading(true);
            const token = localStorage.getItem('token');
            const response = await fetch(`http://localhost:8080/api/customer/orders/${orderId}`, {
                headers: {
                    'Authorization': `Bearer ${token}`
                }
            });

            if (response.ok) {
                const data = await response.json();
                setOrder(data);
                setError('');
            } else if (response.status === 404) {
                setError('Không tìm thấy đơn hàng');
            } else {
                setError('Không thể tải chi tiết đơn hàng');
            }
        } catch (err) {
            setError('Lỗi kết nối: ' + err.message);
        } finally {
            setLoading(false);
        }
    };

    useEffect(() => {
        if (orderId) {
            fetchOrderDetail();
        }
        // eslint-disable-next-line react-hooks/exhaustive-deps
    }, [orderId]);

    const formatDate = (dateString) => {
        return new Date(dateString).toLocaleDateString('vi-VN', {
            year: 'numeric',
            month: '2-digit',
            day: '2-digit',
            hour: '2-digit',
            minute: '2-digit'
        });
    };

    const formatPrice = (price) => {
        return new Intl.NumberFormat('vi-VN', {
            style: 'currency',
            currency: 'VND'
        }).format(price);
    };

    const getCurrentStepIndex = (status) => {
        if (status === 'CANCELLED') return -1;
        return statusSteps.indexOf(status);
    };


    // Sử dụng useCallback để tránh warning về dependency
    const getProductInfo = React.useCallback(async (productId, item) => {
        if (item.product_name && item.product_image_url) {
            return { name: item.product_name, image: item.product_image_url };
        }
        if (productCache[productId]) {
            return productCache[productId];
        }
        try {
            const res = await axios.get(`/api/products/${productId}`);
            const info = {
                name: res.data.name,
                image: res.data.image_url,
            };
            setProductCache((prev) => ({ ...prev, [productId]: info }));
            return info;
        } catch {
            return { name: 'Unknown', image: '' };
        }
    }, [productCache]);

    useEffect(() => {
        if (order && order.items) {
            order.items.forEach(async (item) => {
                await getProductInfo(item.product_id, item);
            });
        }
    }, [order, getProductInfo]);

    if (loading) return <div className="order-detail-loading">Đang tải...</div>;
    if (error) return <div className="order-detail-error">{error}</div>;
    if (!order) return <div className="order-detail-error">Không tìm thấy đơn hàng</div>;

    const currentStepIndex = getCurrentStepIndex(order.status);
     // Xác nhận đã nhận hàng
    const handleConfirmDelivered = async () => {
        if (!orderId) return;
        try {
            setLoading(true);
            const token = localStorage.getItem('token');
            const response = await fetch(`http://localhost:8080/api/customer/orders/${orderId}/confirm-delivery`, {
                method: 'POST',
                headers: {
                    'Authorization': `Bearer ${token}`,
                    'Content-Type': 'application/json'
                }
            });
            if (response.ok) {
                await fetchOrderDetail(); // reload lại chi tiết đơn hàng
            } else {
                const data = await response.text();
                setError(data || 'Không thể xác nhận đã nhận hàng');
            }
        } catch (err) {
            setError('Lỗi kết nối: ' + err.message);
        } finally {
            setLoading(false);
        }
    };

    return (
        <div className="customer-order-detail">
            <div className="order-detail-header">
                <button 
                    className="back-btn"
                    onClick={() => navigate('/customer-orders')}
                >
                    ← Quay lại danh sách đơn hàng
                </button>
                <h2>Chi tiết đơn hàng #{order._id.slice(-8)}</h2>
            </div>
            {/* Order Status Progress */}
            {order.status !== 'CANCELLED' && (
                <div className="status-progress">
                    <h3>Trạng thái đơn hàng</h3>
                    <div className="progress-bar">
                        {statusSteps.map((step, index) => (
                            <div key={step} className="progress-step">
                                <div 
                                    className={`step-circle ${index <= currentStepIndex ? 'completed' : ''}`}
                                    style={{
                                        backgroundColor: index <= currentStepIndex ? statusColors[step] : '#e0e0e0'
                                    }}
                                >
                                    <span className="step-number">{index + 1}</span>
                                </div>
                                <div className="step-label">{statusLabels[step]}</div>
                                {index < statusSteps.length - 1 && (
                                    <div 
                                        className={`step-line ${index < currentStepIndex ? 'completed' : ''}`}
                                    />
                                )}
                            </div>
                        ))}
                    </div>
                </div>
            )}

            {/* Cancelled Status */}
            {order.status === 'CANCELLED' && (
                <div className="status-cancelled">
                    <div className="cancelled-badge">
                        <span>Đơn hàng đã bị hủy</span>
                    </div>
                </div>
            )}

            {/* Order Information */}
            <div className="order-info-section">
                <h3>Thông tin đơn hàng</h3>
                <div className="info-grid">
                    <div className="info-item">
                        <span className="label">Mã đơn hàng:</span>
                        <span className="value">#{order._id}</span>
                    </div>
                    <div className="info-item">
                        <span className="label">Ngày đặt:</span>
                        <span className="value">{formatDate(order.created_at)}</span>
                    </div>
                    <div className="info-item">
                        <span className="label">Trạng thái:</span>
                        <span 
                            className="value status-badge"
                            style={{ 
                                backgroundColor: statusColors[order.status],
                                color: 'white',
                                padding: '4px 8px',
                                borderRadius: '12px',
                                fontSize: '12px'
                            }}
                        >
                            {statusLabels[order.status]}
                        </span>
                    </div>
                    <div className="info-item">
                        <span className="label">Phương thức thanh toán:</span>
                        <span className="value">{order.payment_method}</span>
                    </div>
                </div>
            </div>

            {/* Shipping Information */}
            <div className="shipping-info-section">
                <h3>Thông tin giao hàng</h3>
                <div className="shipping-details">
                    <div className="recipient-info">
                        <p><strong>Người nhận:</strong> {order.fullName}</p>
                        <p><strong>Số điện thoại:</strong> {order.phoneNumber}</p>
                        <p><strong>Địa chỉ:</strong> {order.shipping_address}</p>
                    </div>
                </div>
            </div>

            {/* Order Items */}
            <div className="order-items-section">
                <h3>Sản phẩm đã đặt</h3>
                <div className="items-list">
                    {order.items && order.items.map((item, index) => (
                        <div key={index} className="item-row" style={{display: 'flex', alignItems: 'flex-start', marginBottom: 20, background: '#fff', borderRadius: 8, boxShadow: '0 1px 4px rgba(0,0,0,0.04)', padding: 12}}>
                            {item.image_url && (
                                <img src={item.image_url} alt={item.product_name} style={{width: 72, height: 72, objectFit: 'cover', borderRadius: 6, marginRight: 16, border: '1px solid #eee'}} />
                            )}
                            <div style={{flex: 1, display: 'flex', flexDirection: 'column', justifyContent: 'center'}}>
                                <div style={{display: 'flex', alignItems: 'center', marginBottom: 4}}>
                                    <span className="item-name" style={{fontWeight: 500, fontSize: 16, color: '#222'}}>{item.product_name || 'Tên sản phẩm'}</span>
                                </div>
                                {item.variant && (
                                    <span className="item-variant" style={{color: '#888', fontSize: 13, marginBottom: 2}}>Phân loại hàng: {item.variant}</span>
                                )}
                                <span className="item-quantity" style={{color: '#888', fontSize: 13, marginTop: 2}}>x{item.quantity}</span>
                            </div>
                            <div style={{display: 'flex', flexDirection: 'column', alignItems: 'flex-end', minWidth: 120}}>
                                <span className="item-price" style={{color: '#888', textDecoration: item.old_price ? 'line-through' : 'none', fontSize: 14}}>
                                    {item.old_price ? formatPrice(item.old_price) : ''}
                                </span>
                                <span className="item-total" style={{color: '#ee4d2d', fontWeight: 600, fontSize: 16}}>{formatPrice(item.price * item.quantity)}</span>
                            </div>
                        </div>
                    ))}
                </div>
            </div>

            {/* Order Summary */}
            <div className="order-summary-section">
                <h3>Tổng kết đơn hàng</h3>
                <div className="summary-details">
                    <div className="summary-row">
                        <span>Tổng tiền hàng:</span>
                        <span>{formatPrice(order.total)}</span>
                    </div>
                    <div className="summary-row total">
                        <span><strong>Tổng thanh toán:</strong></span>
                        <span className="total-amount"><strong>{formatPrice(order.total)}</strong></span>
                    </div>
                </div>
            </div>

            {/* Bank Account Info if exists */}
            {order.bankAccount && (
                <div className="bank-info-section">
                    <h3>Thông tin thanh toán</h3>
                    <div className="bank-details">
                        <p><strong>Ngân hàng:</strong> {order.bankAccount.bankName}</p>
                        <p><strong>Chủ tài khoản:</strong> {order.bankAccount.accountHolder}</p>
                        <p><strong>Số tài khoản:</strong> {order.bankAccount.accountNumber}</p>
                    </div>
                </div>
            )}

            {/* Nút xác nhận đã nhận hàng */}
            {order.status === 'SHIPPING' && (
                <div style={{ textAlign: 'right', margin: '16px 0' }}>
                    <button
                        className="confirm-delivered-btn"
                        style={{
                            background: '#27ae60', color: '#fff', border: 'none', borderRadius: 6, padding: '10px 24px', fontWeight: 600, fontSize: 16, cursor: 'pointer', boxShadow: '0 2px 8px rgba(39,174,96,0.08)'
                        }}
                        onClick={handleConfirmDelivered}
                        disabled={loading}
                    >
                        Tôi đã nhận được hàng
                    </button>
                </div>
            )}
        </div>
    );
}

export default CustomerOrderDetail;
