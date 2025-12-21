import React from 'react';
import ReactDOM from 'react-dom/client';
import './index.css';
import App from './App';
import reportWebVitals from './reportWebVitals';

const root = ReactDOM.createRoot(document.getElementById('root'));

// ISKLJUČEN StrictMode da bi sprečili dupli useEffect poziv u development mode-u
root.render(
  // <React.StrictMode>  ← ZAKOMENTIRAJ OVO
    <App />
  // </React.StrictMode>  ← ZAKOMENTIRAJ OVO
);

// If you want to start measuring performance in your app, pass a function
// to log results (for example: reportWebVitals(console.log))
// or send to an analytics endpoint. Learn more: https://bit.ly/CRA-vitals
reportWebVitals();