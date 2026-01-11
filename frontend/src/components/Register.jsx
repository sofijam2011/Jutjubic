import React, { useState } from 'react';
import { useNavigate, Link } from 'react-router-dom';
import authService from '../services/authService';
import './Auth.css';

const Register = () => {
  const navigate = useNavigate();
  const [formData, setFormData] = useState({
    email: '',
    username: '',
    password: '',
    confirmPassword: '',
    firstName: '',
    lastName: '',
    address: ''
  });
  const [errors, setErrors] = useState({});
  const [message, setMessage] = useState('');
  const [loading, setLoading] = useState(false);

  const handleChange = (e) => {
    const { name, value } = e.target;
    setFormData(prev => ({
      ...prev,
      [name]: value
    }));
    // Očisti grešku za to polje kada korisnik počne da kuca
    if (errors[name]) {
      setErrors(prev => ({
        ...prev,
        [name]: ''
      }));
    }
  };

  const validateForm = () => {
    const newErrors = {};

    if (!formData.email) {
      newErrors.email = 'Email je obavezan';
    } else if (!/\S+@\S+\.\S+/.test(formData.email)) {
      newErrors.email = 'Email mora biti validan';
    }

    if (!formData.username) {
      newErrors.username = 'Korisničko ime je obavezno';
    } else if (formData.username.length < 3) {
      newErrors.username = 'Korisničko ime mora imati najmanje 3 karaktera';
    }

    if (!formData.password) {
      newErrors.password = 'Lozinka je obavezna';
    } else if (formData.password.length < 8) {
      newErrors.password = 'Lozinka mora imati najmanje 8 karaktera';
    } else {
      // Provera da li lozinka sadrži sve potrebne elemente
      const hasLowerCase = /[a-z]/.test(formData.password);
      const hasUpperCase = /[A-Z]/.test(formData.password);
      const hasNumber = /[0-9]/.test(formData.password);
      const hasSpecialChar = /[@#$%^&+=!/]/.test(formData.password);

      if (!hasLowerCase) {
        newErrors.password = 'Lozinka mora sadržati bar jedno malo slovo';
      } else if (!hasUpperCase) {
        newErrors.password = 'Lozinka mora sadržati bar jedno veliko slovo';
      } else if (!hasNumber) {
        newErrors.password = 'Lozinka mora sadržati bar jedan broj';
      } else if (!hasSpecialChar) {
        newErrors.password = 'Lozinka mora sadržati bar jedan specijalni karakter (@#$%^&+=!/)';
      }
    }

    if (!formData.confirmPassword) {
      newErrors.confirmPassword = 'Potvrda lozinke je obavezna';
    } else if (formData.password !== formData.confirmPassword) {
      newErrors.confirmPassword = 'Lozinke se ne poklapaju';
    }

    if (!formData.firstName) {
      newErrors.firstName = 'Ime je obavezno';
    }

    if (!formData.lastName) {
      newErrors.lastName = 'Prezime je obavezno';
    }

    if (!formData.address) {
      newErrors.address = 'Adresa je obavezna';
    }

    return newErrors;
  };

  const handleSubmit = async (e) => {
    e.preventDefault();

    const newErrors = validateForm();
    if (Object.keys(newErrors).length > 0) {
      setErrors(newErrors);
      return;
    }

    // DODATO: Ispis podataka u konzolu
    console.log('Sending registration data:', formData);

    setLoading(true);
    setMessage('');
    setErrors({});

    try {
      const response = await authService.register(formData);
      setMessage(response.message);
      setTimeout(() => {
        navigate('/login');
      }, 3000);
    } catch (error) {
      // POBOLJŠAN ERROR HANDLING
      console.log('Registration error:', error);
      console.log('Error response:', error.response);
      console.log('Error data:', error.response?.data);

      if (error.response && error.response.data) {
        if (typeof error.response.data === 'object' && !error.response.data.error) {
          
          setErrors(error.response.data);
        } else if (error.response.data.error) {

          setMessage(error.response.data.error);
        } else {

          setMessage('Greška: ' + JSON.stringify(error.response.data));
        }
      } else {
        setMessage('Greška pri registraciji. Proverite da li je backend pokrenut.');
      }
    } finally {
      setLoading(false);
    }
  };

  return (
      <div className="auth-container">
        <div className="auth-card">
          <h2>Registracija</h2>

          {message && (
              <div className={`message ${message.includes('uspešna') ? 'success' : 'error'}`}>
                {message}
              </div>
          )}

          <form onSubmit={handleSubmit}>
            <div className="form-group">
              <label>Email *</label>
              <input
                  type="email"
                  name="email"
                  value={formData.email}
                  onChange={handleChange}
                  className={errors.email ? 'error' : ''}
              />
              {errors.email && <span className="error-message">{errors.email}</span>}
            </div>

            <div className="form-group">
              <label>Korisničko ime *</label>
              <input
                  type="text"
                  name="username"
                  value={formData.username}
                  onChange={handleChange}
                  className={errors.username ? 'error' : ''}
              />
              {errors.username && <span className="error-message">{errors.username}</span>}
            </div>

            <div className="form-group">
              <label>Ime *</label>
              <input
                  type="text"
                  name="firstName"
                  value={formData.firstName}
                  onChange={handleChange}
                  className={errors.firstName ? 'error' : ''}
              />
              {errors.firstName && <span className="error-message">{errors.firstName}</span>}
            </div>

            <div className="form-group">
              <label>Prezime *</label>
              <input
                  type="text"
                  name="lastName"
                  value={formData.lastName}
                  onChange={handleChange}
                  className={errors.lastName ? 'error' : ''}
              />
              {errors.lastName && <span className="error-message">{errors.lastName}</span>}
            </div>

            <div className="form-group">
              <label>Adresa *</label>
              <input
                  type="text"
                  name="address"
                  value={formData.address}
                  onChange={handleChange}
                  className={errors.address ? 'error' : ''}
              />
              {errors.address && <span className="error-message">{errors.address}</span>}
            </div>

            <div className="form-group">
              <label>Lozinka *</label>
              <input
                  type="password"
                  name="password"
                  value={formData.password}
                  onChange={handleChange}
                  className={`password-input ${errors.password ? 'error' : ''}`}
              />
              {errors.password && <span className="error-message">{errors.password}</span>}
              <small className="password-hint">
                Lozinka mora imati najmanje 8 karaktera, jedno malo slovo, jedno veliko slovo,
                jedan broj i jedan specijalni karakter (@#$%^&+=!/)
              </small>
            </div>

            <div className="form-group">
              <label>Potvrda lozinke *</label>
              <input
                  type="password"
                  name="confirmPassword"
                  value={formData.confirmPassword}
                  onChange={handleChange}
                  className={`password-input ${errors.confirmPassword ? 'error' : ''}`}
              />
              {errors.confirmPassword && <span className="error-message">{errors.confirmPassword}</span>}
            </div>

            <button type="submit" className="btn-primary" disabled={loading}>
              {loading ? 'Registracija u toku...' : 'Registruj se'}
            </button>
          </form>

          <div className="auth-footer">
            Već imate nalog? <Link to="/login">Prijavite se</Link>
          </div>
        </div>
      </div>
  );
};

export default Register;