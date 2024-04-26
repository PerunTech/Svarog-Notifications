/**
 * Import all internal indexes, thus including the code in the final build.
 * export all content representing the surface of your plugin API. Noone is expected to call, but wth.
 * Wait to be called for render, Core will call you.
 */

import Notifications from './components/Notifications';

const routes = [
  {
    name: 'svarog-notifications',
    path: '/main/svarog-notifications',
    render: Notifications,
    isExact: false,
  },
]

export { Notifications, routes }
