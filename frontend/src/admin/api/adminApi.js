import activityApi from './modules/activityApi';
import contentApi from './modules/contentApi';
import dashboardApi from './modules/dashboardApi';
import financeApi from './modules/financeApi';
import hashtagsApi from './modules/hashtagsApi';
import testsApi from './modules/testsApi';
import testSetsApi from './modules/testSetsApi';
import usersApi from './modules/usersApi';

const adminApi = {
    users: usersApi,
    finance: financeApi,
    content: contentApi,
    activity: activityApi,
    dashboard: dashboardApi,
    testSetsApi,
    testsApi,
    hashtagsApi,
};

export { hashtagsApi, testSetsApi, testsApi };
export default adminApi;
