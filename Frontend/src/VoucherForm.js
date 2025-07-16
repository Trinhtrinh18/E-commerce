import React, { useState } from 'react';

function VoucherForm({ voucher, products, onClose, onSave }) {
  const [form, setForm] = useState({
    code: voucher?.code || '',
    discountType: voucher?.discountType || 'PERCENTAGE',
    discountValue: voucher?.discountValue || '',
    minOrderValue: voucher?.minOrderValue || '',
    startDate: voucher?.startDate ? voucher.startDate.substring(0, 10) : '',
    endDate: voucher?.endDate ? voucher.endDate.substring(0, 10) : '',
  });

  const handleChange = e => {
    const { name, value } = e.target;
    setForm(prev => ({ ...prev, [name]: value }));
  };

  const handleSubmit = e => {
    e.preventDefault();
    if (!form.code || !form.discountValue || !form.startDate || !form.endDate) {
      alert('Vui lòng nhập đầy đủ thông tin');
      return;
    }
    // Chuyển đổi ngày sang LocalDateTime string
    const formToSend = {
      ...form,
      startDate: form.startDate.length === 10 ? form.startDate + 'T00:00:00' : form.startDate,
      endDate: form.endDate.length === 10 ? form.endDate + 'T00:00:00' : form.endDate,
    };
    onSave(formToSend);
  };

  return (
    <div className="modal" style={{ position: 'fixed', top: 0, left: 0, right: 0, bottom: 0, background: '#0008', display: 'flex', alignItems: 'center', justifyContent: 'center', zIndex: 4000 }}>
      <form className="voucher-form" onSubmit={handleSubmit} style={{ background: '#fff', borderRadius: 10, padding: 32, minWidth: 320, boxShadow: '0 2px 16px #0002', textAlign: 'center' }}>
        <h3>{voucher ? 'Chỉnh sửa voucher' : 'Thêm voucher mới'}</h3>
        <div className="voucher-form-group">
          <label>Mã voucher</label>
          <input name="code" value={form.code} onChange={handleChange} required className="voucher-input" />
        </div>
        <div className="voucher-form-group">
          <label>Loại giảm giá</label>
          <select name="discountType" value={form.discountType} onChange={handleChange} className="voucher-select">
            <option value="PERCENTAGE">Phần trăm (%)</option>
            <option value="FIXED">Tiền mặt (VNĐ)</option>
          </select>
        </div>
        <div className="voucher-form-group">
          <label>Giá trị giảm</label>
          <input name="discountValue" type="number" value={form.discountValue} onChange={handleChange} required className="voucher-input" />
        </div>
        <div className="voucher-form-group">
          <label>Giá trị đơn tối thiểu</label>
          <input name="minOrderValue" type="number" value={form.minOrderValue || ''} onChange={handleChange} className="voucher-input" min="0" />
        </div>
        <div className="voucher-form-group">
          <label>Ngày bắt đầu</label>
          <input name="startDate" type="date" value={form.startDate} onChange={handleChange} required className="voucher-input" />
        </div>
        <div className="voucher-form-group">
          <label>Ngày kết thúc</label>
          <input name="endDate" type="date" value={form.endDate} onChange={handleChange} required className="voucher-input" />
        </div>
        <div style={{ marginTop: 18, display: 'flex', justifyContent: 'center', gap: 16 }}>
          <button className="save-btn" type="submit">Lưu</button>
          <button className="action-btn" type="button" onClick={onClose}>Hủy</button>
        </div>
      </form>
    </div>
  );
}

export default VoucherForm;
