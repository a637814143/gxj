const hasLetter = value => /[A-Za-z]/.test(value)
const hasDigit = value => /\d/.test(value)
const hasSymbol = value => /[^A-Za-z0-9]/.test(value)

export const isSequentialDigits = password => {
  if (typeof password !== 'string') {
    return false
  }
  const trimmed = password.trim()
  if (!/^\d+$/.test(trimmed) || trimmed.length < 3) {
    return false
  }
  let ascending = true
  let descending = true
  for (let i = 1; i < trimmed.length; i += 1) {
    const prev = Number.parseInt(trimmed[i - 1], 10)
    const current = Number.parseInt(trimmed[i], 10)
    if (current - prev !== 1) {
      ascending = false
    }
    if (current - prev !== -1) {
      descending = false
    }
    if (!ascending && !descending) {
      return false
    }
  }
  return ascending || descending
}

export const validatePasswordPolicy = password => {
  if (!password) {
    return '请输入密码'
  }
  if (password.length < 6) {
    return '密码长度至少为6位'
  }
  if (!hasLetter(password) || !hasDigit(password)) {
    return '密码需同时包含字母和数字'
  }
  if (isSequentialDigits(password)) {
    return '密码不能为连续数字'
  }
  return ''
}

export const getPasswordStrength = password => {
  if (!password) {
    return { level: 'none', label: '未设置', score: 0 }
  }
  let score = 0
  if (password.length >= 6) {
    score += 1
  }
  if (password.length >= 10) {
    score += 1
  }
  if (hasLetter(password) && hasDigit(password)) {
    score += 1
  }
  if (hasSymbol(password)) {
    score += 1
  }
  if (isSequentialDigits(password)) {
    score = 1
  }
  let level = 'weak'
  if (score <= 1) {
    level = 'weak'
  } else if (score === 2 || score === 3) {
    level = 'medium'
  } else {
    level = 'strong'
  }
  const labelMap = {
    none: '未设置',
    weak: '弱',
    medium: '中',
    strong: '强'
  }
  if (password.length === 0) {
    return { level: 'none', label: labelMap.none, score: 0 }
  }
  if (score <= 1) {
    return { level: 'weak', label: labelMap.weak, score: 1 }
  }
  if (score === 2 || score === 3) {
    return { level: 'medium', label: labelMap.medium, score: 2 }
  }
  return { level: 'strong', label: labelMap.strong, score: 3 }
}
