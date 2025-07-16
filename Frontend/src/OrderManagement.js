import React, { useState, useEffect, useCallback } from 'react';
import { useNavigate } from 'react-router-dom';
import './OrderManagement.css';

function OrderManagement() {
    const navigate = useNavigate();
    const [orders, setOrders] = useState([]);
    const [loading, setLoading] = useState(true);
    const [updating, setUpdating] = useState(false);
    const [message, setMessage] = useState('');
    const [statusFilter, setStatusFilter] = useState('');
    const [dateRange, setDateRange] = useState({ start: '', end: '' });
    const [selectedOrder, setSelectedOrder] = useState(null);
    const [showOrderDetail, setShowOrderDetail] = useState(false);
    const [statistics, setStatistics] = useState({});

    const statusOptions = [
        { value: '', label: 'Tất cả trạng thái' },
        { value: 'PENDING', label: 'Chờ xác nhận' },
        { value: 'CONFIRMED', label: 'Đã xác nhận' },
        { value: 'SHIPPING', label: 'Đang giao hàng' },
        { value: 'DELIVERED', label: 'Đã giao hàng' },
        { value: 'CANCELLED', label: 'Đã hủy' }
    ];

    const statusColors = {
        'PENDING': 'status-badge status-pending',
        'CONFIRMED': 'status-badge status-confirmed',
        'SHIPPING': 'status-badge status-shipping',
        'DELIVERED': 'status-badge status-delivered',
        'CANCELLED': 'status-badge status-cancelled'
    };

    // Helper function để get status display (case-insensitive)
    const getStatusDisplay = (status) => {
        if (!status) return '';
        const upperStatus = status.toUpperCase();
        const statusOption = statusOptions.find(s => s.value === upperStatus);
        return statusOption ? statusOption.label : status;
    };

    // Helper function để get status color class (case-insensitive)
    const getStatusColorClass = (status) => {
        if (!status) return 'status-badge';
        const upperStatus = status.toUpperCase();
        return statusColors[upperStatus] || 'status-badge status-default';
    };

    const fetchOrders = useCallback(async () => {
        try {
            setLoading(true);
            const params = new URLSearchParams();
            if (statusFilter) params.append('status', statusFilter);
            if (dateRange.start) params.append('startDate', dateRange.start);
            if (dateRange.end) params.append('endDate', dateRange.end);

            const response = await fetch(`http://localhost:8080/api/seller/orders?${params}`, {
                headers: { 'Authorization': `Bearer ${localStorage.getItem('token')}` }
            });
            
            if (response.ok) {
                const data = await response.json();
                setOrders(data);
                setMessage('');
            } else {
                const errorText = await response.text();
                setMessage(`Lỗi khi lấy danh sách đơn hàng: ${response.status} - ${errorText}`);
            }
        } catch (error) {
            setMessage(`Lỗi mạng: ${error.message}`);
        } finally {
            setLoading(false);
        }
    }, [statusFilter, dateRange.start, dateRange.end]);

    const fetchStatistics = useCallback(async () => {
        try {
            const response = await fetch('http://localhost:8080/api/seller/orders/statistics', {
                headers: { 'Authorization': `Bearer ${localStorage.getItem('token')}` }
            });

            if (response.ok) {
                const data = await response.json();
                setStatistics(data);
            }
        } catch (error) {
            console.error('Error fetching statistics:', error);
        }
    }, []);

    useEffect(() => {
        fetchOrders();
        fetchStatistics();
    }, [fetchOrders, fetchStatistics]);

    const updateOrderStatus = async (orderId, newStatus) => {
        try {
            setUpdating(true);
            const response = await fetch(`http://localhost:8080/api/seller/orders/${orderId}/status`, {
                method: 'PUT',
                headers: {
                    'Content-Type': 'application/json',
                    'Authorization': `Bearer ${localStorage.getItem('token')}`
                },
                body: JSON.stringify({ status: newStatus })
            });

            if (response.ok) {
                // Refresh both orders list and statistics
                await Promise.all([fetchOrders(), fetchStatistics()]);
                
                if (selectedOrder && selectedOrder._id === orderId) {
                    const updatedOrder = await response.json();
                    setSelectedOrder(updatedOrder);
                }
                
                setMessage('Cập nhật trạng thái thành công!');
                setTimeout(() => setMessage(''), 3000); // Clear message after 3 seconds
            } else {
                setMessage('Lỗi khi cập nhật trạng thái');
                setTimeout(() => setMessage(''), 5000);
            }
        } catch (error) {
            console.error('Error updating status:', error);
            setMessage('Lỗi mạng khi cập nhật trạng thái');
            setTimeout(() => setMessage(''), 5000);
        } finally {
            setUpdating(false);
        }
    };

    const viewOrderDetail = async (orderId) => {
        try {
            const response = await fetch(`http://localhost:8080/api/seller/orders/${orderId}`, {
                headers: { 'Authorization': `Bearer ${localStorage.getItem('token')}` }
            });

            if (response.ok) {
                const order = await response.json();
                setSelectedOrder(order);
                setShowOrderDetail(true);
            } else {
                alert('Không thể xem chi tiết đơn hàng');
            }
        } catch (error) {
            console.error('Error fetching order detail:', error);
        }
    };

    const formatCurrency = (amount) => {
        return new Intl.NumberFormat('vi-VN', {
            style: 'currency',
            currency: 'VND'
        }).format(amount);
    };

    const formatDate = (dateString) => {
        return new Date(dateString).toLocaleString('vi-VN');
    };

    return (

        <div className="order-management">
            {/* Header: Back button & Title */}
            <div className="om-header-row">
                <button 
                    className="back-to-shop-btn"
                    onClick={() => navigate('/shop-management')}
                    title="Trở về trang quản lý shop"
                >
                    ← Trở về Quản lý Shop
                </button>
                <h2 className="om-title">Quản lý Đơn hàng</h2>
            </div>

            {/* Statistics Cards */}
            <div className="statistics-cards">
                <div className="stat-card">
                    <h4>Tổng đơn hàng</h4>
                    <span className="stat-number">{statistics.totalOrders || 0}</span>
                </div>
                <div className="stat-card stat-card-success">
                    <h4>Đơn đã hoàn thành</h4>
                    <span className="stat-number">{statistics.completedOrders || 0}</span>
                </div>
                <div className="stat-card stat-card-recent">
                    <h4>Đơn hàng gần đây</h4>
                    <span className="stat-number">{statistics.recentOrders || 0}</span>
                </div>
                <div className="stat-card stat-card-revenue">
                    <h4>Tổng doanh thu</h4>
                    <span className="stat-number">{formatCurrency(statistics.totalRevenue || 0)}</span>
                    <small className="revenue-note">Từ đơn đã giao</small>
                </div>
            </div>

            {/* Filters */}
            <div className="order-filters">
                <div className="filter-group">
                    <label>Trạng thái:</label>
                    <select 
                        value={statusFilter} 
                        onChange={(e) => setStatusFilter(e.target.value)}
                    >
                        {statusOptions.map(option => (
                            <option key={option.value} value={option.value}>
                                {option.label}
                            </option>
                        ))}
                    </select>
                </div>
                <div className="filter-group">
                    <label>Từ ngày:</label>
                    <input 
                        type="date" 
                        value={dateRange.start}
                        onChange={(e) => setDateRange({...dateRange, start: e.target.value})}
                    />
                </div>
                <div className="filter-group">
                    <label>Đến ngày:</label>
                    <input 
                        type="date" 
                        value={dateRange.end}
                        onChange={(e) => setDateRange({...dateRange, end: e.target.value})}
                    />
                </div>
                <button className="filter-reset-btn" onClick={() => {
                    setStatusFilter('');
                    setDateRange({ start: '', end: '' });
                }}>
                    Đặt lại
                </button>
            </div>

            {/* Error/Success Messages */}
            {message && (
                <div className={`alert ${message.includes('Lỗi') ? 'alert-error' : 'alert-success'}`}>
                    {message}
                </div>
            )}

            {/* Updating Status Indicator */}
            {updating && (
                <div className="alert alert-info">
                    <span>Đang cập nhật trạng thái...</span>
                </div>
            )}

            {/* Orders Table */}
            <div className="orders-table-container">
                {loading ? (
                    <div className="loading">Đang tải...</div>
                ) : (
                    <table className="orders-table">
                        <thead>
                            <tr>
                                <th>Mã đơn hàng</th>
                                <th>Khách hàng</th>
                                <th>Ngày đặt</th>
                                <th>Tổng tiền</th>
                                <th>Trạng thái</th>
                                <th>Thao tác</th>
                            </tr>
                        </thead>
                        <tbody>
                            {orders.length === 0 ? (
                                <tr>
                                    <td colSpan="6" className="no-orders">Không có đơn hàng nào</td>
                                </tr>
                            ) : (
                                orders.map(order => (
                                    <tr key={order._id}>
                                        <td className="order-id">{order._id.slice(-8)}</td>
                                        <td>{order.fullName}</td>
                                        <td>{formatDate(order.created_at)}</td>
                                        <td className="order-total">{formatCurrency(order.total)}</td>
                                        <td>
                                            <span className={getStatusColorClass(order.status)}>
                                                {getStatusDisplay(order.status)}
                                            </span>
                                        </td>
                                        <td className="order-actions">
                                            <button 
                                                className="view-btn"
                                                onClick={() => viewOrderDetail(order._id)}
                                            >
                                                Xem
                                            </button>
                                            <select 
                                                value={order.status}
                                                onChange={(e) => updateOrderStatus(order._id, e.target.value)}
                                                className="status-select"
                                                disabled={updating}
                                            >
                                                {statusOptions.slice(1).map(option => (
                                                    <option key={option.value} value={option.value}>
                                                        {option.label}
                                                    </option>
                                                ))}
                                            </select>
                                        </td>
                                    </tr>
                                ))
                            )}
                        </tbody>
                    </table>
                )}
            </div>

            {/* Order Detail Modal */}
            {showOrderDetail && selectedOrder && (
                <div className="modal-overlay" onClick={() => setShowOrderDetail(false)}>
                    <div className="modal-content" onClick={(e) => e.stopPropagation()}>
                        <div className="modal-header">
                            <h3>Chi tiết đơn hàng #{selectedOrder._id.slice(-8)}</h3>
                            <button 
                                className="close-btn"
                                onClick={() => setShowOrderDetail(false)}
                            >
                                ×
                            </button>
                        </div>
                        
                        <div className="modal-body">
                            <div className="order-info-grid">
                                <div className="info-section">
                                    <h4>Thông tin khách hàng</h4>
                                    <p><strong>Tên:</strong> {selectedOrder.fullName}</p>
                                    <p><strong>SĐT:</strong> {selectedOrder.phoneNumber}</p>
                                    <p><strong>Địa chỉ:</strong> {selectedOrder.shipping_address}</p>
                                </div>
                                
                                <div className="info-section">
                                    <h4>Thông tin đơn hàng</h4>
                                    <p><strong>Ngày đặt:</strong> {formatDate(selectedOrder.created_at)}</p>
                                    <p><strong>Trạng thái:</strong> 
                                        <span className={getStatusColorClass(selectedOrder.status)}>
                                            {statusOptions.find(s => s.value === selectedOrder.status)?.label}
                                        </span>
                                    </p>
                                    <p><strong>Phương thức thanh toán:</strong> {selectedOrder.payment_method}</p>
                                </div>
                            </div>
                            
                            <div className="order-items">
                                <h4>Sản phẩm đã đặt</h4>
                                <table className="items-table">
                                    <thead>
                                        <tr>
                                            <th>Sản phẩm</th>
                                            <th>Giá</th>
                                            <th>Số lượng</th>
                                            <th>Thành tiền</th>
                                        </tr>
                                    </thead>
                                    <tbody>
                                        {selectedOrder.items.map((item, index) => (
                                            <tr key={index}>
                                                <td>{item.product_id}</td>
                                                <td>{formatCurrency(item.price)}</td>
                                                <td>{item.quantity}</td>
                                                <td>{formatCurrency(item.price * item.quantity)}</td>
                                            </tr>
                                        ))}
                                    </tbody>
                                </table>
                                
                                <div className="order-total-section">
                                    <h4>Tổng cộng: {formatCurrency(selectedOrder.total)}</h4>
                                </div>
                            </div>
                        </div>
                    </div>
                </div>
            )}
        </div>
    );
}

export default OrderManagement;
