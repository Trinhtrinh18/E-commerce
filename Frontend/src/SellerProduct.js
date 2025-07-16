import React, { useState } from 'react';
import './SellerProduct.css';

function SellerProduct({ product, onClose }) {
  const isEdit = !!product;
  const [form, setForm] = useState({
    name: product?.name || '',
    description: product?.description || '',
    price: product?.price || '',
    stock: product?.stock || '',
    image_url: product?.image_url || '',
    category: product?.category || '',
  });
  const [loading, setLoading] = useState(false);

  const handleChange = e => {
    setForm({ ...form, [e.target.name]: e.target.value });
  };

  const handleSubmit = async e => {
    e.preventDefault();
    setLoading(true);
    const price = Number(form.price);
    const stock = Number(form.stock);
    if (isNaN(price) || isNaN(stock) || price < 0 || stock < 0) {
      alert('Giá và số lượng phải là số hợp lệ và không âm!');
      setLoading(false);
      return;
    }
    const submitForm = { ...form, price, stock };
    try {
      let res;
      const token = localStorage.getItem('token');
      if (isEdit) {
        res = await fetch(`http://localhost:8080/api/seller/products/${product._id}`, {
          method: 'PUT',
          headers: {
            'Content-Type': 'application/json',
            'Authorization': `Bearer ${token}`
          },
          credentials: 'include',
          body: JSON.stringify(submitForm),
        });
      } else {
        res = await fetch('http://localhost:8080/api/seller/products/create', {
          method: 'POST',
          headers: {
            'Content-Type': 'application/json',
            'Authorization': `Bearer ${token}`
          },
          credentials: 'include',
          body: JSON.stringify(submitForm),
        });
      }
      if (!res.ok) throw new Error('Save failed');
      alert(isEdit ? 'Chỉnh sửa sản phẩm thành công!' : 'Thêm sản phẩm mới thành công!');
      onClose(true);
    } catch (err) {
      alert('Lưu thất bại');
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className="modal seller-modal-bg">
      <form className="seller-form-card" onSubmit={handleSubmit}>
        <div className="seller-form-title">{isEdit ? 'Chỉnh sửa sản phẩm' : 'Thêm sản phẩm mới'}</div>
        <div className="seller-form-group">
          <label className="seller-form-label">Hình ảnh (URL)</label>
          <input name="image_url" value={form.image_url} onChange={handleChange} placeholder="Nhập URL ảnh sản phẩm" className="seller-form-input" />
        </div>
        <div className="seller-form-group">
          <label className="seller-form-label">Tên sản phẩm</label>
          <input name="name" value={form.name} onChange={handleChange} className="seller-form-input" required />
        </div>
        <div className="seller-form-group">
          <label className="seller-form-label">Danh mục</label>
          <input name="category" value={form.category} onChange={handleChange} className="seller-form-input" required />
        </div>
        <div className="seller-form-row">
          <div className="seller-form-group half">
            <label className="seller-form-label">Giá (VND)</label>
            <input name="price" value={form.price} onChange={handleChange} type="number" className="seller-form-input" required />
          </div>
          <div className="seller-form-group half">
            <label className="seller-form-label">Số lượng</label>
            <input name="stock" value={form.stock} onChange={handleChange} type="number" className="seller-form-input" required />
          </div>
        </div>
        <div className="seller-form-group">
          <label className="seller-form-label">Mô tả</label>
          <textarea name="description" value={form.description} onChange={handleChange} className="seller-form-input" />
        </div>
        <div className="seller-form-actions">
          <button type="button" className="seller-btn cancel" onClick={() => onClose(false)} disabled={loading}>Hủy</button>
          <button type="submit" className="seller-btn" disabled={loading}>{loading ? 'Đang lưu...' : 'Lưu'}</button>
        </div>
      </form>
    </div>
  );
}

export default SellerProduct;