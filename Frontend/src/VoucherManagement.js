import React, { useEffect, useState } from 'react';
import './VoucherManagement.css';
import VoucherForm from './VoucherForm';

function VoucherManagement({ onClose, onVoucherApplied }) {
  const [vouchers, setVouchers] = useState([]);
  const [products, setProducts] = useState([]);
  const [showForm, setShowForm] = useState(false);
  const [editVoucher, setEditVoucher] = useState(null);
  const [selectedVoucher, setSelectedVoucher] = useState(null);
  const [selectedProducts, setSelectedProducts] = useState([]);
  const [showDeleteModal, setShowDeleteModal] = useState(false);
  const [voucherToDelete, setVoucherToDelete] = useState(null);

  useEffect(() => {
    fetchVouchers();
    fetchProducts();
  }, []);

  const fetchVouchers = async () => {
    const token = localStorage.getItem('token');
    const res = await fetch('http://localhost:8080/api/vouchers/shop', {
      headers: { 'Authorization': `Bearer ${token}` }
    });
    if (res.ok) setVouchers(await res.json());
    else setVouchers([]);
  };

  const fetchProducts = async () => {
    const token = localStorage.getItem('token');
    const res = await fetch('http://localhost:8080/api/seller/products/mine', {
      headers: { 'Authorization': `Bearer ${token}` }
    });
    if (res.ok) setProducts(await res.json());
    else setProducts([]);
  };

  const handleAdd = () => {
    setEditVoucher(null);
    setShowForm(true);
  };

  const handleEdit = (voucher) => {
    setEditVoucher(voucher);
    setShowForm(true);
  };

  const handleDeleteClick = (voucher) => {
    setVoucherToDelete(voucher);
    setShowDeleteModal(true);
  };

  const confirmDeleteVoucher = async () => {
    if (!voucherToDelete) return;
    const token = localStorage.getItem('token');
    const res = await fetch(`http://localhost:8080/api/vouchers/${voucherToDelete.id}`, {
      method: 'DELETE',
      headers: { 'Authorization': `Bearer ${token}` }
    });
    if (res.ok) fetchVouchers();
    setShowDeleteModal(false);
    setVoucherToDelete(null);
  };

  const handleFormClose = () => {
    setShowForm(false);
    setEditVoucher(null);
  };

  const handleSave = async (form) => {
    const token = localStorage.getItem('token');
    const shopId = localStorage.getItem('shopId');
    const method = editVoucher ? 'PUT' : 'POST';
    const url = editVoucher
      ? `http://localhost:8080/api/vouchers/${editVoucher.id}`
      : `http://localhost:8080/api/vouchers`;
    const body = JSON.stringify({ ...form, shopId });
    const res = await fetch(url, {
      method,
      headers: {
        'Content-Type': 'application/json',
        'Authorization': `Bearer ${token}`
      },
      body
    });
    if (res.ok) {
      fetchVouchers();
      setShowForm(false);
      setEditVoucher(null);
      alert(editVoucher ? 'Chỉnh sửa voucher thành công!' : 'Thêm voucher mới thành công!');
    } else {
      alert('Lưu voucher thất bại');
    }
  };

  // Áp dụng voucher cho sản phẩm
  const handleApplyVoucher = async () => {
    if (!selectedVoucher) return;
    if (!selectedProducts || selectedProducts.length === 0) {
      setSelectedVoucher(null);
      setSelectedProducts([]);
      return;
    }
    const token = localStorage.getItem('token');
    const res = await fetch(`http://localhost:8080/api/vouchers/${selectedVoucher.id}`, {
      method: 'PUT',
      headers: {
        'Content-Type': 'application/json',
        'Authorization': `Bearer ${token}`
      },
      body: JSON.stringify({ ...selectedVoucher, productIds: selectedProducts })
    });
    if (res.ok) {
      fetchVouchers();
      if (onVoucherApplied) onVoucherApplied();
      setSelectedVoucher(null);
      setSelectedProducts([]);
      alert('Áp dụng voucher cho sản phẩm thành công!');
    } else {
      alert('Áp dụng voucher thất bại!');
    }
  };

  return (
    <div className="voucher-modal-bg">
      <div className="voucher-modal">
        <div className="voucher-modal-header">
          <h2>Quản lý voucher</h2>
          <button className="close-btn" onClick={onClose}>&times;</button>
        </div>
        <button className="add-voucher-btn" onClick={handleAdd}>+ Thêm voucher</button>
        <table className="voucher-table">
          <thead>
            <tr>
              <th>Mã</th>
              <th>Loại</th>
              <th>Giảm giá</th>
              <th>Ngày bắt đầu</th>
              <th>Ngày kết thúc</th>
              <th>Thao tác</th>
            </tr>
          </thead>
          <tbody>
            {vouchers.map(v => (
              <tr key={v.id} style={{ background: selectedVoucher?.id === v.id ? '#e3f2fd' : undefined }}>
                <td>{v.code}</td>
                <td>{v.discountType === 'PERCENTAGE' ? 'Phần trăm' : 'Tiền mặt'}</td>
                <td>{v.discountType === 'PERCENTAGE' ? v.discountValue + '%' : v.discountValue + 'đ'}</td>
                <td>{v.startDate ? v.startDate.substring(0, 10) : ''}</td>
                <td>{v.endDate ? v.endDate.substring(0, 10) : ''}</td>
                <td>
                  <button className="edit-btn" onClick={() => handleEdit(v)}>Sửa</button>
                  <button className="delete-btn" onClick={() => handleDeleteClick(v)}>Xóa</button>
                  <button style={{ marginLeft: 8 }} onClick={() => {
                    setSelectedVoucher(v);
                    setSelectedProducts(v.productIds || []);
                  }}>Áp dụng</button>
                </td>
              </tr>
            ))}
          </tbody>
        </table>
        {/* Danh sách sản phẩm áp dụng voucher */}
        {selectedVoucher && (
          <div style={{ marginTop: 24, background: '#f8fafc', padding: 16, borderRadius: 8 }}>
            <div style={{ fontWeight: 600, marginBottom: 8 }}>Chọn sản phẩm áp dụng cho voucher <span style={{ color: '#1976d2' }}>{selectedVoucher.code}</span>:</div>
            <div style={{ maxHeight: 200, overflowY: 'auto', border: '1px solid #eee', borderRadius: 4, padding: 8 }}>
              {products.length === 0 && <div>Không có sản phẩm</div>}
              {products.map(p => (
                <label key={p._id} style={{ display: 'block', marginBottom: 4 }}>
                  <input
                    type="checkbox"
                    value={p._id}
                    checked={selectedProducts.includes(p._id)}
                    onChange={e => {
                      if (e.target.checked) {
                        setSelectedProducts([...selectedProducts, p._id]);
                      } else {
                        setSelectedProducts(selectedProducts.filter(id => id !== p._id));
                      }
                    }}
                  />{' '}
                  {p.name}
                </label>
              ))}
            </div>
            <button className="save-btn" style={{ marginTop: 12 }} onClick={handleApplyVoucher}>Áp dụng</button>
            <button style={{ marginLeft: 8 }} onClick={() => { setSelectedVoucher(null); setSelectedProducts([]); }}>Hủy</button>
          </div>
        )}
        {/* Modal xác nhận xóa voucher */}
        {showDeleteModal && (
          <div className="modal" style={{ position: 'fixed', top: 0, left: 0, right: 0, bottom: 0, background: '#0008', display: 'flex', alignItems: 'center', justifyContent: 'center', zIndex: 3000 }}>
            <div style={{ background: '#fff', borderRadius: 10, padding: 32, minWidth: 320, boxShadow: '0 2px 16px #0002', textAlign: 'center' }}>
              <div style={{ fontSize: 18, fontWeight: 600, marginBottom: 18 }}>Xác nhận xóa voucher</div>
              <div style={{ marginBottom: 24 }}>Bạn có chắc muốn xóa voucher này?</div>
              <div style={{ display: 'flex', justifyContent: 'center', gap: 16 }}>
                <button className="action-btn delete-btn" style={{ background: '#e53935', color: '#fff' }} onClick={confirmDeleteVoucher}>Xóa</button>
                <button className="action-btn" onClick={() => { setShowDeleteModal(false); setVoucherToDelete(null); }}>Hủy</button>
              </div>
            </div>
          </div>
        )}
        {/* Modal thêm/sửa voucher */}
        {showForm && (
          <VoucherForm
            voucher={editVoucher}
            products={products}
            onClose={handleFormClose}
            onSave={handleSave}
          />
        )}
      </div>
    </div>
  );
}

export default VoucherManagement;
