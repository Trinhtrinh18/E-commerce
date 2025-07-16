import React, { useState, useEffect, useCallback } from 'react';
import { useNavigate } from 'react-router-dom';
import {
    LineChart,
    Line,
    BarChart,
    Bar,
    PieChart,
    Pie,
    Cell,
    XAxis,
    YAxis,
    CartesianGrid,
    Tooltip,
    Legend,
    ResponsiveContainer
} from 'recharts';
import './RevenueAnalytics.css';

function RevenueAnalytics() {
    const navigate = useNavigate();
    const [revenueData, setRevenueData] = useState({});
    const [chartData, setChartData] = useState([]);
    const [loading, setLoading] = useState(true);
    const [timePeriod, setTimePeriod] = useState('monthly');
    const [message, setMessage] = useState('');

    const fetchRevenueOverview = useCallback(async () => {
        try {
            const response = await fetch('http://localhost:8080/api/seller/revenue/overview', {
                headers: { 'Authorization': `Bearer ${localStorage.getItem('token')}` }
            });

            if (response.ok) {
                const data = await response.json();
                setRevenueData(data);
                setMessage('');
            } else {
                setMessage('Lỗi khi lấy dữ liệu doanh thu');
            }
        } catch (error) {
            setMessage('Lỗi mạng: ' + error.message);
        }
    }, []);

    const fetchChartData = useCallback(async () => {
        try {
            setLoading(true);
            
            // Fetch real chart data from new API endpoint
            const chartResponse = await fetch(`http://localhost:8080/api/seller/revenue/chart?period=${timePeriod}`, {
                headers: { 'Authorization': `Bearer ${localStorage.getItem('token')}` }
            });

            if (chartResponse.ok) {
                const chartResult = await chartResponse.json();
                if (chartResult.data && Array.isArray(chartResult.data)) {
                    setChartData(chartResult.data);
                } else {
                    // Fallback to mock data if API returns unexpected format
                    const mockData = generateMockChartData(timePeriod);
                    setChartData(mockData);
                }
            } else {
                // Fallback to mock data if API fails
                const mockData = generateMockChartData(timePeriod);
                setChartData(mockData);
            }
        } catch (error) {
            setMessage('Lỗi khi lấy dữ liệu biểu đồ: ' + error.message);
            // Fallback to mock data
            const mockData = generateMockChartData(timePeriod);
            setChartData(mockData);
        } finally {
            setLoading(false);
        }
    }, [timePeriod]);

    const generateMockChartData = (period) => {
        const data = [];
        const months = ['T1', 'T2', 'T3', 'T4', 'T5', 'T6', 'T7', 'T8', 'T9', 'T10', 'T11', 'T12'];
        
        if (period === 'monthly') {
            months.forEach(month => {
                const totalOrders = Math.floor(Math.random() * 100) + 20;
                const completedOrders = Math.floor(totalOrders * (0.6 + Math.random() * 0.3)); // 60-90% hoàn thành
                const cancelledOrders = Math.floor(totalOrders * (Math.random() * 0.2)); // 0-20% hủy
                
                data.push({
                    name: month,
                    revenue: Math.floor(Math.random() * 50000000) + 10000000,
                    orders: totalOrders,
                    completedOrders: completedOrders,
                    cancelledOrders: cancelledOrders,
                });
            });
        } else if (period === 'weekly') {
            // Generate last 12 weeks with realistic date format
            const now = new Date();
            for (let i = 11; i >= 0; i--) {
                // Calculate week start (Monday)
                const weekDate = new Date(now);
                weekDate.setDate(weekDate.getDate() - (i * 7));
                const weekStart = new Date(weekDate);
                weekStart.setDate(weekDate.getDate() - weekDate.getDay() + 1); // Monday
                
                const weekEnd = new Date(weekStart);
                weekEnd.setDate(weekStart.getDate() + 6); // Sunday
                
                // Format ngắn gọn hơn: "T25/6-1/7"
                const weekName = `T${weekStart.getDate()}/${weekStart.getMonth() + 1}-${weekEnd.getDate()}/${weekEnd.getMonth() + 1}`;
                
                const totalOrders = Math.floor(Math.random() * 30) + 10;
                const completedOrders = Math.floor(totalOrders * (0.6 + Math.random() * 0.3)); // 60-90% hoàn thành
                const cancelledOrders = Math.floor(totalOrders * (Math.random() * 0.2)); // 0-20% hủy
                
                data.push({
                    name: weekName,
                    revenue: Math.floor(Math.random() * 15000000) + 5000000,
                    orders: totalOrders,
                    completedOrders: completedOrders,
                    cancelledOrders: cancelledOrders,
                });
            }
        } else if (period === 'yearly') {
            // Generate last 5 years
            const currentYear = new Date().getFullYear();
            for (let i = 4; i >= 0; i--) {
                const year = currentYear - i;
                const totalOrders = Math.floor(Math.random() * 1000) + 500;
                const completedOrders = Math.floor(totalOrders * (0.7 + Math.random() * 0.2)); // 70-90% hoàn thành
                const cancelledOrders = Math.floor(totalOrders * (Math.random() * 0.15)); // 0-15% hủy
                
                data.push({
                    name: `Năm ${year}`,
                    revenue: Math.floor(Math.random() * 500000000) + 100000000,
                    orders: totalOrders,
                    completedOrders: completedOrders,
                    cancelledOrders: cancelledOrders,
                });
            }
        }
        
        return data;
    };

    useEffect(() => {
        fetchRevenueOverview();
        fetchChartData();
    }, [fetchRevenueOverview, fetchChartData]);

    const formatCurrency = (amount) => {
        return new Intl.NumberFormat('vi-VN', {
            style: 'currency',
            currency: 'VND'
        }).format(amount);
    };

    const getStatusLabel = (status) => {
        const labels = {
            'PENDING': 'Chờ xác nhận',
            'CONFIRMED': 'Đã xác nhận',
            'SHIPPING': 'Đang giao',
            'DELIVERED': 'Đã giao',
            'CANCELLED': 'Đã hủy'
        };
        // Normalize status to uppercase để đảm bảo matching chính xác
        const normalizedStatus = status ? status.toUpperCase() : '';
        return labels[normalizedStatus] || status;
    };

    const getStatusColor = (status) => {
        const colors = {
            'PENDING': '#FFA726',
            'CONFIRMED': '#66BB6A',
            'SHIPPING': '#42A5F5',
            'DELIVERED': '#26C6DA',
            'CANCELLED': '#EF5350'
        };
        // Normalize status to uppercase để đảm bảo matching chính xác
        const normalizedStatus = status ? status.toUpperCase() : '';
        return colors[normalizedStatus] || '#9E9E9E';
    };

    const statusDistributionData = revenueData.statusDistribution ? 
        Object.entries(revenueData.statusDistribution).map(([status, count]) => ({
            name: getStatusLabel(status),
            value: count,
            status: status
        })) : [];

    // Helper function để format tên hiển thị trên chart
    const formatChartLabel = (name, period) => {
        if (period === 'weekly') {
            // Với weekly, tên đã ngắn rồi (T25/6-1/7), chỉ cần return
            return name;
        }
        return name;
    };

    return (
        <div className="revenue-analytics">
            <div className="revenue-header">
                <div className="header-left">
                    <div className="header-top">
                        <button 
                            className="back-to-shop-btn"
                            onClick={() => navigate('/shop-management')}
                            title="Trở về trang quản lý shop"
                        >
                            ← Trở về Quản lý Shop
                        </button>
                        <h2>Phân tích Doanh thu</h2>
                    </div>
                </div>
                
                {/* Period Filter */}
                <div className="period-filter">
                    <select 
                        value={timePeriod} 
                        onChange={(e) => setTimePeriod(e.target.value)}
                        className="period-select"
                    >
                        <option value="weekly">Theo tuần</option>
                        <option value="monthly">Theo tháng</option>
                        <option value="yearly">Theo năm</option>
                    </select>
                </div>
            </div>

            {message && (
                <div className={`alert ${message.includes('Lỗi') ? 'alert-error' : 'alert-success'}`}>
                    {message}
                </div>
            )}

            {/* Summary Cards */}
            <div className="revenue-summary">
                <div className="summary-card revenue-card">
                    <h4>💰 Tổng Doanh thu</h4>
                    <span className="summary-number">{formatCurrency(revenueData.totalRevenue || 0)}</span>
                    <small>Từ đơn đã giao thành công</small>
                </div>
                <div className="summary-card orders-card">
                    <h4>✅ Đơn hàng hoàn thành</h4>
                    <span className="summary-number">{revenueData.completedOrders || 0}</span>
                    <small>Đơn đã giao thành công</small>
                </div>
                <div className="summary-card conversion-card">
                    <h4>📊 Tỷ lệ hoàn thành</h4>
                    <span className="summary-number">
                        {revenueData.totalOrders ? 
                            Math.round((revenueData.completedOrders / revenueData.totalOrders) * 100) : 0}%
                    </span>
                    <small>Tỷ lệ đơn giao thành công</small>
                </div>
                <div className="summary-card avg-card">
                    <h4>💳 Giá trị trung bình</h4>
                    <span className="summary-number">
                        {revenueData.completedOrders ? 
                            formatCurrency((revenueData.totalRevenue || 0) / revenueData.completedOrders) : 
                            formatCurrency(0)
                        }
                    </span>
                    <small>Giá trị trung bình mỗi đơn</small>
                </div>
            </div>

            {/* Charts Section */}
            <div className="charts-section">
                {/* Revenue Trend Chart */}
                <div className="chart-container">
                    <h3>📈 Xu hướng Doanh thu</h3>
                    {loading ? (
                        <div className="chart-loading">
                            Đang tải dữ liệu biểu đồ...
                        </div>
                    ) : (
                        <ResponsiveContainer width="100%" height={400}>
                            <LineChart data={chartData} margin={{ top: 5, right: 30, left: 20, bottom: 80 }}>
                                <CartesianGrid strokeDasharray="3 3" stroke="#f0f0f0" />
                                <XAxis 
                                    dataKey="name" 
                                    tick={{ fontSize: 10, fill: '#64748b' }}
                                    axisLine={{ stroke: '#e2e8f0' }}
                                    angle={-45}
                                    textAnchor="end"
                                    height={80}
                                    interval={0} // Hiển thị tất cả vì tên đã ngắn
                                    tickFormatter={(value) => formatChartLabel(value, timePeriod)}
                                />
                                <YAxis 
                                    tickFormatter={(value) => `${(value / 1000000).toFixed(0)}M`}
                                    tick={{ fontSize: 12, fill: '#64748b' }}
                                    axisLine={{ stroke: '#e2e8f0' }}
                                />
                                <Tooltip 
                                    formatter={(value, name) => [
                                        name === 'revenue' ? formatCurrency(value) : value,
                                        name === 'revenue' ? 'Doanh thu' : 'Số đơn'
                                    ]}
                                    labelFormatter={(label) => label} // Hiển thị tên đầy đủ trong tooltip
                                    contentStyle={{
                                        background: 'white',
                                        border: '1px solid #e2e8f0',
                                        borderRadius: '12px',
                                        boxShadow: '0 8px 32px rgba(0,0,0,0.1)'
                                    }}
                                />
                                <Legend />
                                <Line 
                                    type="monotone" 
                                    dataKey="revenue" 
                                    stroke="#3b82f6"
                                    strokeWidth={3}
                                    name="Doanh thu"
                                    dot={{ fill: '#3b82f6', strokeWidth: 2, r: 4 }}
                                    activeDot={{ r: 6, stroke: '#3b82f6', strokeWidth: 2 }}
                                />
                            </LineChart>
                        </ResponsiveContainer>
                    )}
                </div>

                {/* Orders Chart */}
                <div className="chart-container">
                    <h3>📦 Thống kê Đơn hàng theo thời gian</h3>
                    {loading ? (
                        <div className="chart-loading">
                            Đang tải dữ liệu biểu đồ...
                        </div>
                    ) : (
                        <ResponsiveContainer width="100%" height={400}>
                            <BarChart data={chartData} margin={{ top: 5, right: 30, left: 20, bottom: 80 }}>
                                <CartesianGrid strokeDasharray="3 3" stroke="#f0f0f0" />
                                <XAxis 
                                    dataKey="name"
                                    tick={{ fontSize: 10, fill: '#64748b' }}
                                    axisLine={{ stroke: '#e2e8f0' }}
                                    angle={-45}
                                    textAnchor="end"
                                    height={80}
                                    interval={0} // Hiển thị tất cả vì tên đã ngắn
                                    tickFormatter={(value) => formatChartLabel(value, timePeriod)}
                                />
                                <YAxis 
                                    tick={{ fontSize: 12, fill: '#64748b' }}
                                    axisLine={{ stroke: '#e2e8f0' }}
                                />
                                <Tooltip 
                                    labelFormatter={(label) => label} // Hiển thị tên đầy đủ trong tooltip
                                    contentStyle={{
                                        background: 'white',
                                        border: '1px solid #e2e8f0',
                                        borderRadius: '12px',
                                        boxShadow: '0 8px 32px rgba(0,0,0,0.1)'
                                    }}
                                />
                                <Legend />
                                <Bar 
                                    dataKey="orders" 
                                    fill="#3b82f6" 
                                    name="Tổng đơn hàng"
                                    radius={[2, 2, 0, 0]}
                                />
                                <Bar 
                                    dataKey="completedOrders" 
                                    fill="#10b981" 
                                    name="Đã hoàn thành"
                                    radius={[2, 2, 0, 0]}
                                />
                                <Bar 
                                    dataKey="cancelledOrders" 
                                    fill="#ef4444" 
                                    name="Đã hủy"
                                    radius={[2, 2, 0, 0]}
                                />
                            </BarChart>
                        </ResponsiveContainer>
                    )}
                </div>

                {/* Status Distribution Chart */}
                <div className="chart-container">
                    <h3>🔄 Phân bố trạng thái Đơn hàng</h3>
                    <ResponsiveContainer width="100%" height={350}>
                        <PieChart>
                            <Pie
                                data={statusDistributionData}
                                cx="50%"
                                cy="50%"
                                labelLine={false}
                                label={({ name, percent }) => 
                                    percent > 5 ? `${name} ${(percent * 100).toFixed(0)}%` : ''
                                }
                                outerRadius={100}
                                fill="#8884d8"
                                dataKey="value"
                            >
                                {statusDistributionData.map((entry, index) => (
                                    <Cell 
                                        key={`cell-${index}`} 
                                        fill={getStatusColor(entry.status)} 
                                    />
                                ))}
                            </Pie>
                            <Tooltip 
                                formatter={(value, name) => [value, name]}
                                contentStyle={{
                                    background: 'white',
                                    border: '1px solid #e2e8f0',
                                    borderRadius: '12px',
                                    boxShadow: '0 8px 32px rgba(0,0,0,0.1)'
                                }}
                            />
                            <Legend 
                                verticalAlign="bottom" 
                                height={36}
                                iconType="circle"
                            />
                        </PieChart>
                    </ResponsiveContainer>
                </div>
            </div>

            {/* Export Section */}
            <div className="export-section">
                <h3>📊 Xuất báo cáo</h3>
                <p style={{ color: '#64748b', marginBottom: '24px' }}>
                    Tải xuống báo cáo doanh thu dưới dạng file để lưu trữ và phân tích
                </p>
                <div className="export-buttons">
                    <button 
                        className="export-btn" 
                        onClick={() => alert('Tính năng xuất PDF đang được phát triển...')}
                    >
                        📄 Xuất PDF
                    </button>
                    <button 
                        className="export-btn" 
                        onClick={() => alert('Tính năng xuất Excel đang được phát triển...')}
                    >
                        📊 Xuất Excel
                    </button>
                </div>
            </div>
        </div>
    );
}

export default RevenueAnalytics;
