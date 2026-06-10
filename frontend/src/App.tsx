import { useState } from 'react';
import { BrowserRouter, Routes, Route } from 'react-router-dom';
import Header from './components/Header';
import StudentList from './pages/StudentList';
import StudentDetail from './pages/StudentDetail';
import AddStudentModal from './components/AddStudentModal';
import './App.css';

function App() {
  const [showAddModal, setShowAddModal] = useState(false);
  const [refreshKey, setRefreshKey] = useState(0);

  const handleAdded = () => {
    setShowAddModal(false);
    setRefreshKey(k => k + 1);
  };

  return (
    <BrowserRouter>
      <div className="app">
        <Header />
        <Routes>
          <Route path="/" element={
            <StudentList
              onAddClick={() => setShowAddModal(true)}
              refreshKey={refreshKey}
            />
          } />
          <Route path="/:id" element={<StudentDetail />} />
        </Routes>
        {showAddModal && (
          <AddStudentModal onClose={() => setShowAddModal(false)} onAdded={handleAdded} />
        )}
      </div>
    </BrowserRouter>
  );
}

export default App;
