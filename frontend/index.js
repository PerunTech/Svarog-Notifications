/**
 * Import all internal indexes, thus including the code in the final build.
 * export all content representing the surface of your plugin API. Noone is expected to call, but wth.
 * Wait to be called for render, Core will call you.
 */

import MessagesWrapper from './components/MessagesWrapper';
import './components/style/search.css'
import './components/style/style.css'

const routes = [
  {
    name: 'svarog-notifications',
    path: '/main/svarog-notifications',
    render: MessagesWrapper,
    isExact: false,
  },
]

export { MessagesWrapper, routes }
