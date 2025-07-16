import React, { useEffect, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import SellerProduct from './SellerProduct';
import VoucherManagement from './VoucherManagement';
import './ShopManagement.css';

function ShopManagement() {
  const navigate = useNavigate();
  const [products, setProducts] = useState([]);
  const [showForm, setShowForm] = useState(false);
  const [editProduct, setEditProduct] = useState(null);
  const [search, setSearch] = useState('');
  const [tab, setTab] = useState('all');
  const [detailProduct, setDetailProduct] = useState(null);
  const [categories, setCategories] = useState([]);
  const [selectedCategory, setSelectedCategory] = useState('all');
  const [sortField, setSortField] = useState('');
  const [sortOrder, setSortOrder] = useState('asc');
  const [deleteProductId, setDeleteProductId] = useState(null);
  const [showDeleteModal, setShowDeleteModal] = useState(false);
  const [showVoucherModal, setShowVoucherModal] = useState(false);
  const [vouchers, setVouchers] = useState([]);

  // Thêm class để override CSS chỉ khi ShopManagement active
  useEffect(() => {
    // Thêm class ngay khi component mount
    document.body.classList.add('shop-management-active');
    
    // Đảm bảo scroll được reset về top
    window.scrollTo(0, 0);
    
    // Cleanup function để loại bỏ class khi component unmount
    return () => {
      document.body.classList.remove('shop-management-active');
      // Reset overflow cho body khi thoát
      document.body.style.overflow = '';
    };
  }, []);

  useEffect(() => {
    fetchProducts();
    fetchCategories();
    fetchVouchers();
  }, []);

  const fetchProducts = async () => {
    try {
      const token = localStorage.getItem('token');
      const res = await fetch('http://localhost:8080/api/seller/products/mine', {
        credentials: 'include',
        headers: {
          'Content-Type': 'application/json',
          'Authorization': `Bearer ${token}`
        }
      });
      if (!res.ok) throw new Error('Network response was not ok');
      const data = await res.json();
      setProducts(data);
    } catch (err) {
      alert('Không thể tải sản phẩm');
    }
  };

  const fetchCategories = async () => {
    try {
      const res = await fetch('http://localhost:8080/api/products/categories');
      if (!res.ok) throw new Error('Network response was not ok');
      const data = await res.json();
      setCategories(data);
    } catch (err) {
      console.error('Failed to fetch categories', err);
    }
  };

  const fetchVouchers = async () => {
    const token = localStorage.getItem('token');
    const res = await fetch('http://localhost:8080/api/vouchers/shop', {
      headers: { 'Authorization': `Bearer ${token}` }
    });
    if (res.ok) {
      setVouchers(await res.json());
    } else {
      setVouchers([]);
    }
  };

  const handleEdit = (product) => {
    setEditProduct(product);
    setShowForm(true);
  };

  const handleAdd = () => {
    setEditProduct(null);
    setShowForm(true);
  };

  const handleFormClose = (reload) => {
    setShowForm(false);
    setEditProduct(null);
    if (reload) fetchProducts();
  };

  // Lọc sản phẩm theo tab và category
  const filteredProducts = products.filter(p => {
    let matchTab = true;
    if (tab === 'all') matchTab = true;
    if (selectedCategory !== 'all' && p.category !== selectedCategory) return false;
    if (search && !p.name.toLowerCase().includes(search.toLowerCase())) return false;
    return matchTab;
  });

  // Sắp xếp sản phẩm theo trường được chọn
  const sortedProducts = [...filteredProducts].sort((a, b) => {
    if (!sortField) return 0;
    let aValue = a[sortField] || 0;
    let bValue = b[sortField] || 0;
    if (typeof aValue === 'string') aValue = aValue.toLowerCase();
    if (typeof bValue === 'string') bValue = bValue.toLowerCase();
    if (aValue < bValue) return sortOrder === 'asc' ? -1 : 1;
    if (aValue > bValue) return sortOrder === 'asc' ? 1 : -1;
    return 0;
  });

  // Hàm xử lý khi click vào tiêu đề cột để sắp xếp
  function handleSort(field) {
    if (sortField === field) {
      setSortOrder(sortOrder === 'asc' ? 'desc' : 'asc');
    } else {
      setSortField(field);
      setSortOrder('asc');
    }
  }

  // Hàm xóa sản phẩm
  function handleDelete(id) {
    setDeleteProductId(id);
    setShowDeleteModal(true);
  }
  function confirmDelete() {
    const id = deleteProductId;
    setShowDeleteModal(false);
    setDeleteProductId(null);
    if (!id) return;
    try {
      const token = localStorage.getItem('token');
      fetch(`http://localhost:8080/api/seller/products/${id}`, {
        method: 'DELETE',
        credentials: 'include',
        headers: {
          'Content-Type': 'application/json',
          'Authorization': `Bearer ${token}`
        }
      }).then(res => {
        if (!res.ok) throw new Error('Delete failed');
        setProducts(products => products.filter(p => p._id !== id));
      }).catch(() => alert('Xóa thất bại'));
    } catch (err) {
      alert('Xóa thất bại');
    }
  }

  // Lấy danh sách category duy nhất từ sản phẩm
  const uniqueCategories = Array.from(new Set(products.map(p => p.category).filter(Boolean)));

  // Thêm hàm xử lý hủy áp dụng voucher
  function handleRemoveVoucher(productId, voucherId) {
    const token = localStorage.getItem('token');
    if (!voucherId) {
      alert('Không tìm thấy voucher để hủy');
      return;
    }
    fetch('http://localhost:8080/api/vouchers/remove-product', {
      method: 'PATCH',
      headers: {
        'Content-Type': 'application/json',
        'Authorization': `Bearer ${token}`
      },
      body: JSON.stringify({ voucherId, productId })
    })
      .then(async res => {
        const text = await res.text();
        if (!res.ok) throw new Error(text || 'Hủy áp dụng voucher thất bại');
        fetchVouchers();
        alert('Hủy áp dụng voucher thành công');
      })
      .catch((err) => alert('Hủy áp dụng voucher thất bại: ' + err.message));
  }

  return (
    <>
      <div className="shop-management-wrapper">
        <div className="product-management-page">
          <div className="sidebar">
            <div className="sidebar-title">🏪 Quản Lý Shop</div>
            <div className="sidebar-item active">📦 Tất Cả Sản Phẩm</div>
            <div 
              className="sidebar-item"
              onClick={() => navigate('/order-management')}
              style={{ cursor: 'pointer' }}
              title="Chuyển đến trang quản lý đơn hàng"
            >
              📋 Quản lý Đơn hàng
            </div>
            <div 
              className="sidebar-item"
              onClick={() => navigate('/revenue-analytics')}
              style={{ cursor: 'pointer' }}
              title="Chuyển đến trang báo cáo doanh thu"
            >
              📊 Báo cáo Doanh thu
            </div>
          </div>
          <div className="main-content">
            {/* Fixed Headers */}
            <div className="fixed-headers">
              <div className="header-row">
                <div className="tabs">
                  <select
                    id="category-select"
                    className="category-select"
                    value={selectedCategory}
                    onChange={e => setSelectedCategory(e.target.value)}
                  >
                    <option value="all">Tất cả</option>
                    {uniqueCategories.map(cat => (
                      <option key={cat} value={cat}>{cat}</option>
                    ))}
                  </select>
                </div>
                <div className="header-actions">
                  <button className="header-btn" onClick={() => setShowVoucherModal(true)}>
                    🎫 Quản lý voucher
                  </button>
                  <button className="add-btn" onClick={handleAdd}>
                    ➕ Thêm sản phẩm mới
                  </button>
                </div>
              </div>
              <div className="filter-row">
                <input
                  className="search-input"
                  placeholder="🔍 Tìm kiếm tên sản phẩm, SKU..."
                  value={search}
                  onChange={e => setSearch(e.target.value)}
                />
                <button className="filter-btn">🔎 Tìm kiếm</button>
              </div>
            </div>
            
            {/* Scrollable Content */}
            <div className="scrollable-content">
              <div className="product-table-wrapper">
          <table className="product-table">
            <thead>
              <tr>
                <th>✅</th>
                <th>📋 Tên sản phẩm</th>
                <th style={{ cursor: 'pointer' }} onClick={() => handleSort('price')}>
                  💰 Giá {sortField === 'price' && (sortOrder === 'asc' ? '↑' : '↓')}
                </th>
                <th style={{ cursor: 'pointer' }} onClick={() => handleSort('stock')}>
                  📦 Kho hàng {sortField === 'stock' && (sortOrder === 'asc' ? '↑' : '↓')}
                </th>
                <th>🎫 Voucher áp dụng</th>
                <th>⚙️ Thao tác</th>
              </tr>
            </thead>
            <tbody>
              {sortedProducts.map(product => (
                <tr key={product._id}>
                  <td><input type="checkbox" className="checkbox-input" /></td>
                  <td>
                    <div className="product-info">
                      <img src={product.image_url} alt={product.name} className="product-img larger" />
                      <div>
                        <div className="product-name">{product.name}</div>
                      </div>
                    </div>
                  </td>
                  <td>
                    <span className="price-display">
                      {product.price?.toLocaleString('vi-VN')} ₫
                    </span>
                  </td>
                  <td>
                    <span className={`stock-display ${product.stock === 0 ? 'out' : product.stock < 10 ? 'low' : ''}`}>
                      {product.stock === 0 ? 'Hết hàng' : `${product.stock} sản phẩm`}
                    </span>
                  </td>
                  <td>
                    {(() => {
                      const applicableVouchers = vouchers.filter(v => Array.isArray(v.productIds) && v.productIds.includes(product._id));
                      if (applicableVouchers.length > 0) {
                        return (
                          <div className="voucher-container">
                            {applicableVouchers.map((voucher, index) => (
                              <div key={voucher.id} className="voucher-item">
                                <span className="voucher-code">{voucher.code}</span>
                                <button
                                  className="voucher-remove-btn"
                                  onClick={() => handleRemoveVoucher(product._id, voucher.id)}
                                >
                                  Hủy
                                </button>
                              </div>
                            ))}
                          </div>
                        );
                      } else {
                        return <span className="no-voucher">Chưa áp dụng</span>;
                      }
                    })()}
                  </td>
                  <td>
                    <div className="action-btn-group">
                      <button 
                        className="action-btn" 
                        onClick={() => handleEdit(product)}
                        title="Chỉnh sửa sản phẩm"
                      >
                        ✏️ Sửa
                      </button>
                      <button 
                        className="action-btn" 
                        onClick={() => setDetailProduct(product)}
                        title="Xem chi tiết sản phẩm"
                      >
                        👁️ Xem
                      </button>
                      <button 
                        className="action-btn delete-btn" 
                        onClick={() => handleDelete(product._id)}
                        title="Xóa sản phẩm"
                      >
                        🗑️ Xóa
                      </button>
                    </div>
                  </td>
                </tr>
              ))}
              {sortedProducts.length === 0 && (
                <tr>
                  <td colSpan="6" style={{ textAlign: 'center', color: '#718096', fontStyle: 'italic', padding: '40px' }}>
                    📭 Không có sản phẩm nào
                  </td>
                </tr>
              )}
            </tbody>
          </table>
            </div>
          </div>
        </div>
      </div>
    </div>
      {showForm && <SellerProduct product={editProduct} onClose={handleFormClose} />}
      {detailProduct && (
        <div className="modal" style={{ 
          position: 'fixed', top: 0, left: 0, right: 0, bottom: 0, 
          background: 'rgba(0,0,0,0.6)', display: 'flex', alignItems: 'center', 
          justifyContent: 'center', zIndex: 2000, backdropFilter: 'blur(4px)' 
        }}>
          <div style={{ 
            background: '#fff', borderRadius: 20, padding: 32, minWidth: 400, maxWidth: 500, 
            boxShadow: '0 20px 60px rgba(0,0,0,0.3)', position: 'relative',
            maxHeight: '90vh', overflowY: 'auto'
          }}>
            <button 
              onClick={() => setDetailProduct(null)} 
              style={{ 
                position: 'absolute', top: 16, right: 20, background: 'none', 
                border: 'none', fontSize: 28, cursor: 'pointer', color: '#666',
                transition: 'color 0.3s ease'
              }}
              onMouseEnter={(e) => e.target.style.color = '#e53e3e'}
              onMouseLeave={(e) => e.target.style.color = '#666'}
            >
              ✕
            </button>
            {detailProduct.image_url && (
              <img 
                src={detailProduct.image_url} 
                alt={detailProduct.name} 
                style={{ 
                  width: '100%', height: 240, objectFit: 'cover', borderRadius: 16, 
                  marginBottom: 20, border: '2px solid #e9ecef'
                }} 
              />
            )}
            <div style={{ 
              fontWeight: 700, fontSize: 24, color: '#2d3748', marginBottom: 12,
              lineHeight: 1.3
            }}>
              {detailProduct.name}
            </div>
            <div style={{ 
              fontWeight: 700, color: '#e53e3e', fontSize: 20, marginBottom: 12,
              background: 'rgba(229, 62, 62, 0.1)', padding: '8px 16px', 
              borderRadius: 8, display: 'inline-block'
            }}>
              {detailProduct.price?.toLocaleString('vi-VN')} ₫
            </div>
            <div style={{ 
              color: detailProduct.stock === 0 ? '#e53e3e' : detailProduct.stock < 10 ? '#ed8936' : '#38a169', 
              fontSize: 16, marginBottom: 16, fontWeight: 600,
              background: detailProduct.stock === 0 ? 'rgba(229, 62, 62, 0.1)' : 
                         detailProduct.stock < 10 ? 'rgba(237, 137, 54, 0.1)' : 'rgba(56, 161, 105, 0.1)',
              padding: '8px 16px', borderRadius: 8, display: 'inline-block'
            }}>
              {detailProduct.stock === 0 ? '📦 Hết hàng' : `📦 Tồn kho: ${detailProduct.stock} sản phẩm`}
            </div>
            <div style={{ fontSize: 16, marginBottom: 16, color: '#4a5568' }}>
              <strong>🏷️ Danh mục:</strong> 
              <span style={{ 
                background: 'rgba(102, 126, 234, 0.1)', color: '#667eea', 
                padding: '4px 12px', borderRadius: 8, marginLeft: 8, fontWeight: 600
              }}>
                {detailProduct.category}
              </span>
            </div>
            <div style={{ 
              fontSize: 15, marginBottom: 20, maxHeight: 180, overflowY: 'auto', 
              paddingRight: 8, color: '#4a5568', lineHeight: 1.6,
              background: '#f8f9fa', padding: 16, borderRadius: 12
            }}>
              <strong>📝 Mô tả:</strong><br />
              {detailProduct.description}
            </div>
            {(detailProduct.updated_at || detailProduct.created_at) && (
              <div style={{ 
                fontSize: 13, color: '#718096', marginBottom: 16,
                background: '#f1f3f4', padding: '8px 12px', borderRadius: 8
              }}>
                <strong>🕒 Cập nhật lần cuối:</strong> {
                  detailProduct.updated_at ? 
                    new Date(detailProduct.updated_at).toLocaleString('vi-VN') : 
                    new Date(detailProduct.created_at).toLocaleString('vi-VN')
                }
              </div>
            )}
          </div>
        </div>
      )}
      {showDeleteModal && (
        <div className="modal" style={{ 
          position: 'fixed', top: 0, left: 0, right: 0, bottom: 0, 
          background: 'rgba(0,0,0,0.6)', display: 'flex', alignItems: 'center', 
          justifyContent: 'center', zIndex: 3000, backdropFilter: 'blur(4px)' 
        }}>
          <div style={{ 
            background: '#fff', borderRadius: 20, padding: 40, minWidth: 380, 
            boxShadow: '0 20px 60px rgba(0,0,0,0.3)', textAlign: 'center' 
          }}>
            <div style={{ 
              fontSize: 48, marginBottom: 20, color: '#e53e3e' 
            }}>
              🗑️
            </div>
            <div style={{ 
              fontSize: 20, fontWeight: 600, marginBottom: 16, color: '#2d3748' 
            }}>
              Xác nhận xóa sản phẩm
            </div>
            <div style={{ 
              marginBottom: 32, color: '#4a5568', fontSize: 16, lineHeight: 1.5 
            }}>
              Bạn có chắc muốn xóa sản phẩm này không?<br />
              <span style={{ color: '#e53e3e', fontWeight: 600 }}>
                Hành động này không thể hoàn tác!
              </span>
            </div>
            <div style={{ display: 'flex', justifyContent: 'center', gap: 16 }}>
              <button 
                style={{ 
                  background: 'linear-gradient(135deg, #e53e3e 0%, #c53030 100%)', 
                  color: '#fff', border: 'none', padding: '12px 24px', 
                  borderRadius: 12, fontSize: 14, fontWeight: 600, cursor: 'pointer',
                  transition: 'all 0.3s ease', textTransform: 'uppercase', letterSpacing: '0.5px'
                }} 
                onClick={confirmDelete}
                onMouseEnter={(e) => e.target.style.transform = 'translateY(-2px)'}
                onMouseLeave={(e) => e.target.style.transform = 'translateY(0)'}
              >
                🗑️ Xóa ngay
              </button>
              <button 
                style={{ 
                  background: '#fff', color: '#667eea', border: '2px solid #667eea', 
                  padding: '12px 24px', borderRadius: 12, fontSize: 14, fontWeight: 600, 
                  cursor: 'pointer', transition: 'all 0.3s ease', textTransform: 'uppercase',
                  letterSpacing: '0.5px'
                }} 
                onClick={() => setShowDeleteModal(false)}
                onMouseEnter={(e) => {
                  e.target.style.background = '#667eea';
                  e.target.style.color = '#fff';
                  e.target.style.transform = 'translateY(-2px)';
                }}
                onMouseLeave={(e) => {
                  e.target.style.background = '#fff';
                  e.target.style.color = '#667eea';
                  e.target.style.transform = 'translateY(0)';
                }}
              >
                ↩️ Hủy bỏ
              </button>
            </div>
          </div>
        </div>
      )}
      {showVoucherModal && <VoucherManagement onClose={() => setShowVoucherModal(false)} onVoucherApplied={fetchVouchers} />}
    </>
  );
}

export default ShopManagement;