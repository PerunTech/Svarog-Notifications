/**
 * When running a dev environment, you need to register with Core.
 * Import your plugin assembly, i.e. whatever you exported from your entry file, 
 * and call the pluginManager with:
 *  - The name of your plugin, by convention the name specified in package.json
 *  - Your plugin implementation, assembled as an object.
 */
import pkg from '../package.json'
import { pluginManager } from 'perun-core'
import * as plugin from './index'

pluginManager.registerPlugin(pkg.name, plugin)
