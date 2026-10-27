export function getMainLabel(labelCode, intl) {
  return intl.formatMessage({
    id: `perun.svarog_notifications.${labelCode}`,
    defaultMessage: `perun.svarog_notifications.${labelCode}`
  })
}
