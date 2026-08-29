function escapeHtml(text) {
  return text
    .replace(/&/g, '&amp;')
    .replace(/</g, '&lt;')
    .replace(/>/g, '&gt;')
}

function renderInline(text) {
  return text
    .replace(/\*\*\*(.+?)\*\*\*/g, '<strong><em>$1</em></strong>')
    .replace(/\*\*(.+?)\*\*/g, '<strong>$1</strong>')
    .replace(/\*(.+?)\*/g, '<em>$1</em>')
    .replace(/`(.+?)`/g, '<code>$1</code>')
}

function renderTable(block) {
  const lines = block.split('\n').filter(l => l.trim())
  if (lines.length < 2) return '<p>' + escapeHtml(block) + '</p>'
  const parseRow = (line) => line.replace(/^\|/, '').replace(/\|$/, '').split('|').map(s => s.trim())
  const header = parseRow(lines[0])
  const rows = lines.slice(2).map(parseRow)
  let html = '<table><thead><tr>'
  header.forEach(h => { html += '<th>' + renderInline(escapeHtml(h)) + '</th>' })
  html += '</tr></thead><tbody>'
  rows.forEach(row => {
    html += '<tr>'
    row.forEach(cell => { html += '<td>' + renderInline(escapeHtml(cell)) + '</td>' })
    html += '</tr>'
  })
  html += '</tbody></table>'
  return html
}

function renderBlock(block) {
  block = block.trim()
  if (!block) return ''

  const hrMatch = block.match(/^(?:---+|\*\*\*+|___+)\s*$/)
  if (hrMatch) return '<hr>'

  const headingMatch = block.match(/^(#{1,6})\s+(.+)$/)
  if (headingMatch) {
    const level = headingMatch[1].length
    return '<h' + level + '>' + renderInline(escapeHtml(headingMatch[2])) + '</h' + level + '>'
  }

  if (block.includes('|') && /\|[\s-:]+\|/.test(block.split('\n')[1] || '')) {
    return renderTable(block)
  }

  const bqMatch = block.match(/^(?:>\s?.+\n?)+/)
  if (bqMatch) {
    const inner = block.replace(/^>\s?/gm, '')
    return '<blockquote>' + renderBlocks(inner) + '</blockquote>'
  }

  if (/^[-*+]\s/.test(block)) {
    const items = block.split('\n').filter(l => /^[-*+]\s/.test(l))
    return '<ul>' + items.map(l => '<li>' + renderInline(escapeHtml(l.replace(/^[-*+]\s+/, ''))) + '</li>').join('') + '</ul>'
  }

  if (/^\d+\.\s/.test(block)) {
    const items = block.split('\n').filter(l => /^\d+\.\s/.test(l))
    return '<ol>' + items.map(l => '<li>' + renderInline(escapeHtml(l.replace(/^\d+\.\s+/, ''))) + '</li>').join('') + '</ol>'
  }

  if (/^```/.test(block)) {
    const code = block.replace(/^```\w*\n?/, '').replace(/\n?```$/, '')
    return '<pre><code>' + escapeHtml(code) + '</code></pre>'
  }

  const lines = block.split('\n')
  const paragraphs = []
  let current = []
  for (const line of lines) {
    if (line.trim() === '') {
      if (current.length) paragraphs.push(current.join('\n'))
      current = []
    } else {
      current.push(line)
    }
  }
  if (current.length) paragraphs.push(current.join('\n'))

  if (paragraphs.length <= 1) {
    return '<p>' + renderInline(escapeHtml(block.replace(/\n/g, ' '))) + '</p>'
  }
  return paragraphs.map(p => '<p>' + renderInline(escapeHtml(p.replace(/\n/g, ' '))) + '</p>').join('')
}

function renderBlocks(text) {
  if (!text) return ''
  const blocks = text.split(/\n{2,}/)
  return blocks.map(renderBlock).filter(Boolean).join('\n')
}

export function renderMarkdown(text) {
  if (!text) return ''
  try {
    return renderBlocks(text)
  } catch (e) {
    console.error('[renderMarkdown] error:', e)
    return '<p>' + escapeHtml(text) + '</p>'
  }
}
