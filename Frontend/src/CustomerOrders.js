import React, { useState, useEffect } from 'react';
import { useNavigate, useParams } from 'react-router-dom';
import './CustomerOrders.css';

function CustomerOrders() {
    // Xác nhận đã nhận hàng từ danh sách
    const handleConfirmDelivered = async (orderId) => {
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
                await fetchOrders(selectedStatus); // reload lại danh sách đơn hàng
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
    // Hủy đơn hàng từ danh sách
    const handleCancelOrder = async (orderId) => {
        if (!orderId) return;
        if (!window.confirm('Bạn có chắc muốn hủy đơn hàng này?')) return;
        try {
            setLoading(true);
            const token = localStorage.getItem('token');
            const response = await fetch(`http://localhost:8080/api/customer/orders/${orderId}/cancel`, {
                method: 'POST',
                headers: {
                    'Authorization': `Bearer ${token}`,
                    'Content-Type': 'application/json'
                }
            });
            if (response.ok) {
                await fetchOrders(selectedStatus); // reload lại danh sách đơn hàng
            } else {
                const data = await response.text();
                setError(data || 'Không thể hủy đơn hàng');
            }
        } catch (err) {
            setError('Lỗi kết nối: ' + err.message);
        } finally {
            setLoading(false);
        }
    };
    const navigate = useNavigate();
    const [orders, setOrders] = useState([]);
    const [loading, setLoading] = useState(true);
    const [error, setError] = useState('');
    const [selectedStatus, setSelectedStatus] = useState('ALL');

    const statusLabels = {
        'ALL': 'Tất cả',
        'PENDING': 'Chờ xác nhận',
        'CONFIRMED': 'Đã xác nhận',
        'SHIPPING': 'Đang giao',
        'DELIVERED': 'Đã giao',
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

    const fetchOrders = async (status = 'ALL') => {
        try {
            setLoading(true);
            const token = localStorage.getItem('token');
            const response = await fetch(`http://localhost:8080/api/customer/orders?status=${status}`, {
                headers: {
                    'Authorization': `Bearer ${token}`
                }
            });

            if (response.ok) {
                const data = await response.json();
                setOrders(data);
                setError('');
            } else {
                setError('Không thể tải danh sách đơn hàng');
            }
        } catch (err) {
            setError('Lỗi kết nối: ' + err.message);
        } finally {
            setLoading(false);
        }
    };

    useEffect(() => {
        fetchOrders(selectedStatus);
    }, [selectedStatus]);

    const handleStatusChange = (status) => {
        setSelectedStatus(status);
    };

    const handleOrderDetail = (orderId) => {
        navigate(`/customer-orders/${orderId}`);
    };

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

    if (loading) return <div className="customer-orders-loading">Đang tải...</div>;
    if (error) return <div className="customer-orders-error">{error}</div>;

    return (
        <div className="customer-orders">
            <div className="customer-orders-header">
                <h2>Đơn hàng của tôi</h2>
                <button 
                    className="back-btn"
                    onClick={() => navigate('/dashboard')}
                >
                    ← Quay lại
                </button>
            </div>

            {/* Status Filter Tabs */}
            <div className="status-tabs">
                {Object.entries(statusLabels).map(([status, label]) => (
                    <button
                        key={status}
                        className={`status-tab ${selectedStatus === status ? 'active' : ''}`}
                        onClick={() => handleStatusChange(status)}
                    >
                        {label}
                    </button>
                ))}
            </div>

            {/* Orders List */}
            <div className="orders-container">
                {orders.length === 0 ? (
                    <div className="no-orders">
                        <p>Không có đơn hàng nào</p>
                        <button 
                            className="continue-shopping-btn"
                            onClick={() => navigate('/products')}
                        >
                            Tiếp tục mua sắm
                        </button>
                    </div>
                ) : (
                    orders.map(order => (
                        <div key={order._id} className="order-card">
                            <div className="order-header">
                                <div className="order-info">
                                    <span className="order-id">Đơn hàng #{order._id.slice(-8)}</span>
                                    <span className="order-date">{formatDate(order.created_at)}</span>
                                </div>
                                <div 
                                    className="order-status"
                                    style={{ 
                                        backgroundColor: statusColors[order.status] || '#95a5a6',
                                        color: 'white',
                                        padding: '4px 12px',
                                        borderRadius: '16px',
                                        fontSize: '12px',
                                        fontWeight: 'bold'
                                    }}
                                >
                                    {statusLabels[order.status] || order.status}
                                </div>
                            </div>

                            <div className="order-items">
                                {order.items && order.items.slice(0, 2).map((item, index) => (
                                    <div key={index} className="order-item">
                                        <div className="item-info" style={{display: 'flex', flexDirection: 'column', alignItems: 'flex-start'}}>
                                            <span className="item-name" style={{fontWeight: 500, fontSize: 16, marginBottom: 2}}>{item.product_name}</span>
                                            {/* Nếu có phân loại hàng, hiển thị ở đây */}
                                            {item.variant && (
                                                <span className="item-variant" style={{color: '#888', fontSize: 13}}>Phân loại hàng: {item.variant}</span>
                                            )}
                                            {item.image_url && (
                                                <img src={item.image_url} alt={item.product_name} className="item-image" style={{maxWidth: 60, maxHeight: 60, margin: '8px 0 0 0', borderRadius: 4}} />
                                            )}
                                            <span className="item-quantity" style={{color: '#888', fontSize: 13, marginTop: 2}}>x{item.quantity}</span>
                                        </div>
                                        <span className="item-price">{formatPrice(item.price)}</span>
                                    </div>
                                ))}
                                {order.items && order.items.length > 2 && (
                                    <div className="more-items">
                                        +{order.items.length - 2} sản phẩm khác
                                    </div>
                                )}
                            </div>

                            <div className="order-footer">
                                <div className="order-total">
                                    <span>Tổng tiền: <strong>{formatPrice(order.total)}</strong></span>
                                </div>
                                <div style={{ display: 'flex', gap: 8 }}>
                                    {order.status === 'SHIPPING' && (
                                        <button
                                            className="confirm-delivered-btn"
                                            style={{
                                                background: '#27ae60', color: '#fff', border: 'none', borderRadius: 6, padding: '8px 16px', fontWeight: 600, fontSize: 14, cursor: 'pointer', boxShadow: '0 2px 8px rgba(39,174,96,0.08)'
                                            }}
                                            onClick={() => handleConfirmDelivered(order._id)}
                                            disabled={loading}
                                        >
                                            Đã nhận được hàng
                                        </button>
                                    )}
                    {(order.status === 'PENDING' || order.status === 'CONFIRMED') && (
                        <button
                            className="cancel-order-btn"
                            style={{
                                background: '#e74c3c', color: '#fff', border: 'none', borderRadius: 6, padding: '8px 16px', fontWeight: 600, fontSize: 14, cursor: 'pointer', boxShadow: '0 2px 8px rgba(231,76,60,0.08)'
                            }}
                            onClick={() => handleCancelOrder(order._id)}
                            disabled={loading}
                        >
                            Hủy đơn hàng
                        </button>
                    )}
                                    <button 
                                        className="view-detail-btn"
                                        onClick={() => handleOrderDetail(order._id)}
                                    >
                                        Xem chi tiết
                                    </button>
                                </div>
                            </div>
                        </div>
                    ))
                )}
            </div>
        </div>
    );
}

export default CustomerOrders;
