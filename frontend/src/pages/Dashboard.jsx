import { useEffect, useState } from 'react';
import { useAuth } from '../contexts/AuthContext';
import { sectionApi, userAnswerApi } from '../api/backendApi';
import { useNavigate } from 'react-router-dom';

export default function Dashboard() {
  const { user, profile } = useAuth();
  const [sections, setSections] = useState([]);
  const [stats, setStats] = useState(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);
  const navigate = useNavigate();

  useEffect(() => {
    loadDashboardData();
  }, [user]);

  const loadDashboardData = async () => {
    try {
      setLoading(true);
      setError(null); // Clear previous errors
      
      // Load sections
      try {
        const sectionsResponse = await sectionApi.getAll();
        setSections(sectionsResponse.data);
      } catch (sectionError) {
        console.error('Error loading sections:', sectionError);
        setSections([]); // Set empty array on error
        
        // Check if it's a network error
        if (sectionError.code === 'ERR_NETWORK') {
          setError('Cannot connect to server. Please make sure the backend is running.');
        }
      }

      // Load user stats if user is logged in
      if (user?.id) {
        try {
          const statsResponse = await userAnswerApi.getUserStats(user.id);
          setStats(statsResponse.data);
        } catch (statsError) {
          // User might not have any answers yet or backend is down
          console.log('No stats available:', statsError.message);
          setStats(null);
        }
      }
    } catch (error) {
      console.error('Error loading dashboard data:', error);
      setError('Failed to load dashboard data. Please try again.');
    } finally {
      // Always set loading to false, even if there are errors
      setLoading(false);
    }
  };

  if (loading) {
    return (
      <div className="min-h-screen flex items-center justify-center">
        <div className="text-xl">Loading...</div>
      </div>
    );
  }

  return (
    <div className="min-h-screen bg-gray-50">
      {/* Header */}
      <header className="bg-white shadow">
        <div className="max-w-7xl mx-auto py-6 px-4 sm:px-6 lg:px-8">
          <h1 className="text-3xl font-bold text-gray-900">
            Cramer IELTS Practice
          </h1>
        </div>
      </header>

      {/* Main Content */}
      <main className="max-w-7xl mx-auto py-6 sm:px-6 lg:px-8">
        {/* Error Message */}
        {error && (
          <div className="mb-6 bg-red-50 border-l-4 border-red-400 p-4">
            <div className="flex items-center justify-between">
              <div className="flex">
                <div className="flex-shrink-0">
                  <svg className="h-5 w-5 text-red-400" viewBox="0 0 20 20" fill="currentColor">
                    <path fillRule="evenodd" d="M10 18a8 8 0 100-16 8 8 0 000 16zM8.707 7.293a1 1 0 00-1.414 1.414L8.586 10l-1.293 1.293a1 1 0 101.414 1.414L10 11.414l1.293 1.293a1 1 0 001.414-1.414L11.414 10l1.293-1.293a1 1 0 00-1.414-1.414L10 8.586 8.707 7.293z" clipRule="evenodd" />
                  </svg>
                </div>
                <div className="ml-3">
                  <p className="text-sm text-red-700">{error}</p>
                </div>
              </div>
              <button
                onClick={loadDashboardData}
                className="ml-4 px-4 py-2 bg-red-600 text-white text-sm font-medium rounded hover:bg-red-700 focus:outline-none focus:ring-2 focus:ring-red-500"
              >
                Retry
              </button>
            </div>
          </div>
        )}

        {/* Stats Section */}
        {stats && (
          <div className="mb-8 grid grid-cols-1 md:grid-cols-4 gap-4">
            <div className="bg-white overflow-hidden shadow rounded-lg">
              <div className="px-4 py-5 sm:p-6">
                <dt className="text-sm font-medium text-gray-500 truncate">
                  Total Answers
                </dt>
                <dd className="mt-1 text-3xl font-semibold text-gray-900">
                  {stats.totalAnswers}
                </dd>
              </div>
            </div>
            
            <div className="bg-white overflow-hidden shadow rounded-lg">
              <div className="px-4 py-5 sm:p-6">
                <dt className="text-sm font-medium text-gray-500 truncate">
                  Correct Answers
                </dt>
                <dd className="mt-1 text-3xl font-semibold text-green-600">
                  {stats.correctAnswers}
                </dd>
              </div>
            </div>
            
            <div className="bg-white overflow-hidden shadow rounded-lg">
              <div className="px-4 py-5 sm:p-6">
                <dt className="text-sm font-medium text-gray-500 truncate">
                  Incorrect Answers
                </dt>
                <dd className="mt-1 text-3xl font-semibold text-red-600">
                  {stats.incorrectAnswers}
                </dd>
              </div>
            </div>
            
            <div className="bg-white overflow-hidden shadow rounded-lg">
              <div className="px-4 py-5 sm:p-6">
                <dt className="text-sm font-medium text-gray-500 truncate">
                  Accuracy
                </dt>
                <dd className="mt-1 text-3xl font-semibold text-indigo-600">
                  {stats.accuracy.toFixed(1)}%
                </dd>
              </div>
            </div>
          </div>
        )}

        {/* Sections List */}
        <div className="bg-white shadow overflow-hidden sm:rounded-md">
          <div className="px-4 py-5 sm:px-6 border-b border-gray-200">
            <h2 className="text-lg leading-6 font-medium text-gray-900">
              Available Practice Tests
            </h2>
            <p className="mt-1 text-sm text-gray-500">
              {sections.length} sections available
            </p>
          </div>
          
          <ul className="divide-y divide-gray-200">
            {sections.length === 0 ? (
              <li className="px-4 py-4 text-center text-gray-500">
                No sections available yet
              </li>
            ) : (
              sections.map((section) => (
                <li key={section.id} className="px-4 py-4 hover:bg-gray-50">
                  <div className="flex items-center justify-between">
                    <div className="flex-1 min-w-0">
                      <p className="text-sm font-medium text-indigo-600 truncate">
                        {section.examSource.toUpperCase()} - Test {section.testNumber} - Part {section.partNumber}
                      </p>
                      <p className="text-sm text-gray-500">
                        {section.skill.charAt(0).toUpperCase() + section.skill.slice(1)}
                      </p>
                    </div>
                    <div>
                      <button
                        onClick={() => navigate(`/practice/${section.id}`)}
                        className="inline-flex items-center px-4 py-2 border border-transparent text-sm font-medium rounded-md text-white bg-indigo-600 hover:bg-indigo-700"
                      >
                        Start Practice
                      </button>
                    </div>
                  </div>
                </li>
              ))
            )}
          </ul>
        </div>
      </main>
    </div>
  );
}
