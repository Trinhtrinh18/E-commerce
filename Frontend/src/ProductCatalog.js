import React, { useState, useEffect } from 'react';
import { useNavigate } from 'react-router-dom';
import './ProductCatalog.css';

function ProductCatalog({ onAddToCart, userId, searchTerm, setSearchTerm, sortOption, setSortOption, isSearching, setIsSearching }) {
    const navigate = useNavigate();
    const [products, setProducts] = useState([]);
    const [message, setMessage] = useState('');
    const [categories, setCategories] = useState([]);
    const [showSuggestions, setShowSuggestions] = useState(false);
    const [visibleRows, setVisibleRows] = useState(5);

    const fetchCategories = async () => {
        try {
            const response = await fetch('http://localhost:8080/api/products/categories');
            if (response.ok) {
                const data = await response.json();
                setCategories(data);
            } else {
                console.error('Error fetching categories:', response.statusText);
            }
        } catch (error) {
            console.error('Network error fetching categories:', error.message);
        }
    };

    const fetchProducts = async (term = '', category = '', sort = '', forRecommendation = false) => {
        try {
            let url;
            let currentProducts = [];

            if (forRecommendation && !term && !category && !sort) {
                if (!userId) {
                    setMessage('User ID not available for recommendations.');
                    setProducts([]);
                    return;
                }
                url = `http://localhost:8080/api/recommendations`;
                const response = await fetch(url, {
                    headers: { 'Authorization': `Bearer ${localStorage.getItem('token')}` }
                });
                if (response.ok) {
                    const data = await response.json();
                    currentProducts = data.data || [];
                    setMessage('');
                } else {
                    setMessage(`Error fetching recommendations: ${response.statusText}`);
                }
            } else {
                const params = new URLSearchParams();
                if (term) {
                    params.append('keyword', term);
                }
                if (category) {
                    params.append('category', category);
                }
                if (sort) {
                    params.append('sort', sort);
                }

                if (params.toString()) {
                    url = `http://localhost:8080/api/products/search?${params.toString()}`;
                } else {
                    url = 'http://localhost:8080/api/products/all';
                }

                const response = await fetch(url);
                if (response.ok) {
                    currentProducts = await response.json();
                    setMessage('');
                } else {
                    setMessage(`Error fetching products: ${response.statusText}`);
                }
            }
            setProducts(currentProducts);

        } catch (error) {
            setMessage(`Network error fetching products/recommendations: ${error.message}`);
        }
    };

    useEffect(() => {
        if (isSearching) {
            fetchProducts(searchTerm, '', sortOption, false);
        } else {
            fetchProducts('', '', sortOption, !!userId);
        }
        fetchCategories();
        setVisibleRows(5);
        // eslint-disable-next-line
    }, [userId, searchTerm, sortOption, isSearching]);

    // Listen for order completion to refresh product list
    useEffect(() => {
        const handleOrderCompletion = () => {
            // Refresh product list after order completion
            if (isSearching) {
                fetchProducts(searchTerm, '', sortOption, false);
            } else {
                fetchProducts('', '', sortOption, !!userId);
            }
        };
        
        window.addEventListener('orderCompleted', handleOrderCompletion);
        
        return () => {
            window.removeEventListener('orderCompleted', handleOrderCompletion);
        };
    }, [isSearching, searchTerm, sortOption, userId]);

    const handleSearchTermChange = (e) => {
        setSearchTerm(e.target.value);
        setIsSearching(true);
    };

    const handleSearchSubmit = (e) => {
        e.preventDefault();
        if (searchTerm.trim() === '') {
            setIsSearching(false);
            fetchProducts('', '', '', true);
        } else {
            setIsSearching(true);
            fetchProducts(searchTerm, '', sortOption);
        }
        setShowSuggestions(false);
    };

    const handleCategoryClick = (categoryName) => {
        setSearchTerm(categoryName);
        setIsSearching(true);
        fetchProducts('', categoryName, sortOption);
        setShowSuggestions(false);
    };

    const handleSortChange = (e) => {
        const newSortOption = e.target.value;
        setSortOption(newSortOption);
        setIsSearching(true);
        fetchProducts(searchTerm, '', newSortOption);
    };

    const productsPerRow = 4;
    const visibleCount = visibleRows * productsPerRow;
    const displayedProducts = (!isSearching && userId)
        ? products.slice(0, visibleCount)
        : products;

    return (
        <div className="product-catalog-container">
            <h2>
                {isSearching
                    ? "Kết quả tìm kiếm"
                    : userId
                        ? "Sản phẩm gợi ý cho bạn"
                        : "Tất cả sản phẩm"
                }
            </h2>
            <form onSubmit={handleSearchSubmit} className="search-bar">
                <input
                    type="text"
                    placeholder="Tìm kiếm sản phẩm..."
                    value={searchTerm}
                    onChange={handleSearchTermChange}
                    onFocus={() => setShowSuggestions(true)}
                    onBlur={() => setTimeout(() => setShowSuggestions(false), 100)}
                />
                <button type="submit">Tìm kiếm</button>
            </form>

            {isSearching && showSuggestions && categories.length > 0 && (
                <div className="category-suggestions">
                    <h3>Danh mục gợi ý:</h3>
                    <ul>
                        {categories.slice(0, 6).map(category => (
                            <li key={category} onClick={() => handleCategoryClick(category)}>
                                {category}
                            </li>
                        ))}
                    </ul>
                </div>
            )}

            {isSearching && (
                <div className="sort-options">
                    <label htmlFor="sort">Sắp xếp theo:</label>
                    <select id="sort" value={sortOption} onChange={handleSortChange}>
                        <option value="">Mặc định</option>
                        <option value="priceAsc">Giá: Thấp đến Cao</option>
                        <option value="priceDesc">Giá: Cao đến Thấp</option>
                        <option value="newest">Mới nhất</option>
                        <option value="oldest">Cũ nhất</option>
                    </select>
                </div>
            )}

            {message && <p className="message error">{message}</p>}

            <div className="product-list">
                {displayedProducts.length > 0 ? (
                    displayedProducts.map(product => (
                        <div key={product._id} className="product-item">
                            <div
                                className="product-image-container"
                                style={product.image_url ? { backgroundImage: `url(${product.image_url})` } : {}}
                            >
                                {!product.image_url && <div className="no-image">Không có hình ảnh</div>}
                                {product.stock === 0 && (
                                    <img src="/soldout.png" alt="Sold Out" className="sold-out-overlay" />
                                )}
                            </div>
                            <h3>{product.name}</h3>
                            <div className="product-meta">
                                <p className="stock">Tồn kho: {product.stock}</p>
                                <p className="sold-count">Đã bán: {product.purchaseCount || 0}</p>
                            </div>
                            <p className="price">{product.price.toLocaleString('vi-VN')} VND</p>
                            <div className="actions">
                                <button onClick={() => navigate(`/product/${product._id}`)} className="secondary-btn">Xem chi tiết</button>
                            </div>
                        </div>
                    ))
                ) : (
                    <p>Không tìm thấy sản phẩm nào.</p>
                )}
            </div>

            {!isSearching && userId && displayedProducts.length < products.length && (
                <div style={{ textAlign: 'center', margin: '32px 0' }}>
                    <button
                        className="see-more-btn"
                        onClick={() => setVisibleRows(visibleRows + 4)}
                    >
                        Xem thêm
                    </button>
                </div>
            )}
        </div>
    );
}

export default ProductCatalog;