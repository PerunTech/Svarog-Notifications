import { ComponentManager, redux } from 'perun-core'

export function flattenObject (obj) {
  const flattened = {}
  Object.keys(obj).forEach((key) => {
    if (typeof obj[key] === 'object' && obj[key] !== null) {
      Object.assign(flattened, flattenObject(obj[key]))
    } else {
      flattened[key] = obj[key]
    }
  })
  return flattened
}

/**
 * Converts a JSON object to an encoded URI string
 * @param  {object} json The JSON object that needs to be converted
 */
export function jsonToURI (json) {
  let arr = []
  for (let property in json) {
    if (Object.prototype.hasOwnProperty.call(json, property) && json[property] !== undefined) {
      arr.push(encodeURIComponent(property) + '=' + encodeURIComponent(json[property]))
    }
  }
  return arr.join('&')
}

export function resetFormSave(tableName) { debugger
  ComponentManager.setStateForComponent(`${tableName}_FORM`, null)
}