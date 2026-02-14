import React, { useState, useEffect } from 'react';
import { useNavigate } from 'react-router-dom';
import commentService from '../services/commentService';
import './CommentSection.css';

const CommentSection = ({ videoId }) => {
    const navigate = useNavigate();

    const [comments, setComments] = useState([]);
    const [loading, setLoading] = useState(true);
    const [error, setError] = useState('');

    const [currentPage, setCurrentPage] = useState(0);
    const [totalPages, setTotalPages] = useState(0);
    const [totalComments, setTotalComments] = useState(0);
    const [hasNext, setHasNext] = useState(false);
    const [hasPrevious, setHasPrevious] = useState(false);

    const [commentText, setCommentText] = useState('');
    const [submitting, setSubmitting] = useState(false);
    const [submitError, setSubmitError] = useState('');
    const [submitSuccess, setSubmitSuccess] = useState('');

    const [remainingComments, setRemainingComments] = useState(null);

    const PAGE_SIZE = 10;
    const MAX_COMMENT_LENGTH = 1000;

    useEffect(() => {
        loadComments(currentPage);
    }, [videoId, currentPage]);

    useEffect(() => {
        if (localStorage.getItem('token')) {
            loadRateLimitStatus();
        }
    }, [videoId]);

    const loadComments = async (page) => {
        try {
            setLoading(true);
            setError('');
            const data = await commentService.getComments(videoId, page, PAGE_SIZE);

            setComments(data.comments || []);
            setCurrentPage(data.currentPage || 0);
            setTotalPages(data.totalPages || 0);
            setTotalComments(data.totalComments || 0);
            setHasNext(data.hasNext || false);
            setHasPrevious(data.hasPrevious || false);
        } catch (err) {
            console.error('Error loading comments:', err);
            setError('Greška pri učitavanju komentara');
        } finally {
            setLoading(false);
        }
    };

    const loadRateLimitStatus = async () => {
        try {
            const remaining = await commentService.getRateLimitStatus(videoId);
            setRemainingComments(remaining);
        } catch (err) {
            console.error('Error loading rate limit:', err);
        }
    };

    const handleSubmitComment = async (e) => {
        e.preventDefault();

        const token = localStorage.getItem('token');

        if (!token) {
            alert('Morate biti prijavljeni da biste ostavili komentar!');
            return;
        }

        if (!commentText.trim()) {
            setSubmitError('Komentar ne može biti prazan');
            return;
        }

        if (commentText.length > MAX_COMMENT_LENGTH) {
            setSubmitError(`Komentar ne može biti duži od ${MAX_COMMENT_LENGTH} karaktera`);
            return;
        }

        setSubmitting(true);
        setSubmitError('');
        setSubmitSuccess('');

        try {
            await commentService.addComment(videoId, commentText.trim());
            setCommentText('');
            setSubmitSuccess('Komentar je uspešno dodat!');

            await loadComments(0);
            await loadRateLimitStatus();

            setTimeout(() => setSubmitSuccess(''), 3000);

        } catch (err) {
            console.error('Error submitting comment:', err);
            const errorMsg = err.response?.data?.error || 'Greška pri dodavanju komentara';
            setSubmitError(errorMsg);

            if (errorMsg.includes('limit')) {
                await loadRateLimitStatus();
            }
        } finally {
            setSubmitting(false);
        }
    };

    const handlePageChange = (newPage) => {
        setCurrentPage(newPage);
        window.scrollTo({ top: 0, behavior: 'smooth' });
    };

    const formatDate = (dateString) => {
        const date = new Date(dateString);
        const now = new Date();
        const diffInSeconds = Math.floor((now - date) / 1000);

        if (diffInSeconds < 60) return 'Upravo sada';
        if (diffInSeconds < 3600) return `Pre ${Math.floor(diffInSeconds / 60)} min`;
        if (diffInSeconds < 86400) return `Pre ${Math.floor(diffInSeconds / 3600)} h`;
        if (diffInSeconds < 2592000) return `Pre ${Math.floor(diffInSeconds / 86400)} dana`;

        return date.toLocaleDateString('sr-RS', {
            day: 'numeric',
            month: 'long',
            year: 'numeric'
        });
    };

    const getRateLimitClass = () => {
        if (remainingComments === null) return '';
        if (remainingComments === 0) return 'rate-limit-error';
        if (remainingComments <= 10) return 'rate-limit-warning';
        return '';
    };

    const getCharCounterClass = () => {
        const length = commentText.length;
        if (length > MAX_COMMENT_LENGTH) return 'error';
        if (length > MAX_COMMENT_LENGTH * 0.9) return 'warning';
        return '';
    };

    return (
        <div className="comments-section">
            <div className="comments-header">
                <h2>💬 Komentari</h2>
                <span className="comment-count">
                    {totalComments} {totalComments === 1 ? 'komentar' : 'komentara'}
                </span>
            </div>

            {localStorage.getItem('token') ? (
                <div className="add-comment-form">
                    {remainingComments !== null && (
                        <div className={`rate-limit-info ${getRateLimitClass()}`}>
                            {remainingComments === 0 ? (
                                <>⚠️ Dostigli ste limit od 60 komentara po satu. Pokušajte ponovo kasnije.</>
                            ) : remainingComments <= 10 ? (
                                <>⚠️ Preostalo komentara u ovom satu: <strong>{remainingComments}/60</strong></>
                            ) : (
                                <>ℹ️ Preostalo komentara: <strong>{remainingComments}/60</strong></>
                            )}
                        </div>
                    )}

                    {submitError && (
                        <div className="comments-error" style={{ marginBottom: '1rem' }}>
                            {submitError}
                        </div>
                    )}

                    {submitSuccess && (
                        <div style={{
                            padding: '1rem',
                            background: '#d4edda',
                            border: '1px solid #c3e6cb',
                            borderRadius: '8px',
                            color: '#155724',
                            marginBottom: '1rem'
                        }}>
                            ✓ {submitSuccess}
                        </div>
                    )}

                    <form onSubmit={handleSubmitComment}>
                        <textarea
                            className="comment-textarea"
                            placeholder="Napišite komentar..."
                            value={commentText}
                            onChange={(e) => setCommentText(e.target.value)}
                            disabled={submitting || remainingComments === 0}
                            maxLength={MAX_COMMENT_LENGTH + 100}
                        />

                        <div className="comment-form-actions">
                            <span className={`char-counter ${getCharCounterClass()}`}>
                                {commentText.length} / {MAX_COMMENT_LENGTH}
                            </span>

                            <button
                                type="submit"
                                className="submit-comment-btn"
                                disabled={
                                    submitting ||
                                    !commentText.trim() ||
                                    commentText.length > MAX_COMMENT_LENGTH ||
                                    remainingComments === 0
                                }
                            >
                                {submitting ? '⏳ Slanje...' : '📤 Objavi komentar'}
                            </button>
                        </div>
                    </form>
                </div>
            ) : (
                <div className="auth-message">
                    💡 <a href="/login">Prijavite se</a> da biste mogli da komentarišete
                </div>
            )}

            {loading ? (
                <div className="comments-loading">Učitavanje komentara...</div>
            ) : error ? (
                <div className="comments-error">{error}</div>
            ) : comments.length === 0 ? (
                <div className="no-comments">
                    <div className="no-comments-icon">💬</div>
                    <p>Još nema komentara. Budite prvi koji će komentarisati!</p>
                </div>
            ) : (
                <>
                    <div className="comments-list">
                        {comments.map((comment) => (
                            <div key={comment.id} className="comment-item">
                                <div className="comment-header">
                                    <span
                                        className="comment-author"
                                        onClick={() => navigate(`/user/${comment.userId}`)}
                                    >
                                        @{comment.username}
                                    </span>
                                    <span className="comment-date">
                                        {formatDate(comment.createdAt)}
                                    </span>
                                </div>
                                <p className="comment-text">{comment.text}</p>
                            </div>
                        ))}
                    </div>

                    {totalPages > 1 && (
                        <div className="pagination">
                            <button
                                className="pagination-btn"
                                onClick={() => handlePageChange(currentPage - 1)}
                                disabled={!hasPrevious}
                            >
                                ← Prethodna
                            </button>

                            <span className="pagination-info">
                                Stranica {currentPage + 1} od {totalPages}
                            </span>

                            <button
                                className="pagination-btn"
                                onClick={() => handlePageChange(currentPage + 1)}
                                disabled={!hasNext}
                            >
                                Sledeća →
                            </button>
                        </div>
                    )}
                </>
            )}
        </div>
    );
};

export default CommentSection;
