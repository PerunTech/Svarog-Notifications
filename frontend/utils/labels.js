export function getMainLabel(labelCode, context) {
  return context.intl.formatMessage({
    id: `perun.svarog_notifications.${labelCode}`,
    defaultMessage: `perun.svarog_notifications.${labelCode}`
  })
}
