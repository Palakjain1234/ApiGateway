const colours = {
  ACTIVE:    'bg-green-100 text-green-800',
  PENDING:   'bg-yellow-100 text-yellow-800',
  SUSPENDED: 'bg-orange-100 text-orange-800',
  DELETED:   'bg-red-100 text-red-800',
  true:      'bg-green-100 text-green-800',
  false:     'bg-gray-100 text-gray-600',
}

export default function Badge({ value }) {
  const label  = typeof value === 'boolean' ? (value ? 'Active' : 'Inactive') : value
  const colour = colours[String(value)] ?? 'bg-gray-100 text-gray-600'
  return (
    <span className={`inline-flex px-2 py-0.5 rounded-full text-xs font-medium ${colour}`}>
      {label}
    </span>
  )
}
