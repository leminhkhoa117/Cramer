import { useEffect, useState } from 'react';
import { useAuth } from '../contexts/AuthContext';
import { sectionApi, userAnswerApi } from '../api/backendApi';
import { useNavigate } from 'react-router-dom';

export default function Dashboard() {
  const { user, profile, signOut } = useAuth();
  const [sections, setSections] = useState([]);
  const [stats, setStats] = useState(null);
  const [loading, setLoading] = useState(true);
  const navigate = useNavigate();

  useEffect(() => {
    loadDashboardData();
  }, [user]);

  const loadDashboardData = async () => {
    try {
      setLoading(true);
      
      // Load sections
      const sectionsResponse = await sectionApi.getAll();
      setSections(sectionsResponse.data);

      // Load user stats if user is logged in
      if (user?.id) {
        try {
          const statsResponse = await userAnswerApi.getUserStats(user.id);
          setStats(statsResponse.data);
        } catch (error) {
          // User might not have any answers yet
          console.log('No stats available yet');
        }
      }
    } catch (error) {
      console.error('Error loading dashboard data:', error);
    } finally {
      setLoading(false);
    }
  };

  const handleSignOut = async () => {
    await signOut();
    navigate('/login');
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
        <div className="max-w-7xl mx-auto py-6 px-4 sm:px-6 lg:px-8 flex justify-between items-center">
          <h1 className="text-3xl font-bold text-gray-900">
            Cramer IELTS Practice
          </h1>
          <div className="flex items-center space-x-4">
            <span className="text-gray-700">
              Welcome, {profile?.username || user?.email}!
            </span>
            <button
              onClick={handleSignOut}
              className="bg-red-500 hover:bg-red-600 text-white px-4 py-2 rounded-md text-sm"
            >
              Sign Out
            </button>
          </div>
        </div>
      </header>

      {/* Main Content */}
      <main className="max-w-7xl mx-auto py-6 sm:px-6 lg:px-8">
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
