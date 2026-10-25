#!/usr/bin/env python3
"""Turns the CurseForge page text into HTML that pastes into the page's rich-text editor.

  python tools/pack-description-to-html.py

Reads everything under "## Description" in pack/launch/curseforge-description.md and writes pack/launch/curseforge-description.html.
Open the HTML in a browser, select all, copy, and paste into the project's description box (a markdown paste would show its symbols).
It understands only what that file uses: headings, paragraphs, bullets (with wrapped lines), one-level quotes, tables, **bold**, `code`, [links](https://...).
"""

import html
import os
import re

HERE = os.path.dirname(os.path.abspath(__file__))
SRC = os.path.join(HERE, '..', 'pack', 'launch', 'curseforge-description.md')
OUT = os.path.join(HERE, '..', 'pack', 'launch', 'curseforge-description.html')


def inline(text):
    text = html.escape(text, quote=False)
    text = re.sub(r'\[([^\]]+)\]\((https?://[^)\s]+)\)', r'<a href="\2">\1</a>', text)
    text = re.sub(r'\*\*(.+?)\*\*', r'<strong>\1</strong>', text)
    text = re.sub(r'`(.+?)`', r'<code>\1</code>', text)
    return text


def convert(lines):
    out, para, items, quote = [], [], [], []

    def flush():
        nonlocal para, items, quote
        if para:
            out.append('<p>' + inline(' '.join(para)) + '</p>')
        if items:
            out.append('<ul>' + ''.join('<li>' + inline(' '.join(i)) + '</li>' for i in items) + '</ul>')
        if quote:
            out.append('<blockquote><p>' + inline(' '.join(quote)) + '</p></blockquote>')
        para, items, quote = [], [], []

    i = 0
    while i < len(lines):
        line = lines[i].rstrip()
        if not line.strip():
            flush()
        elif line.startswith('#'):
            flush()
            level = len(line) - len(line.lstrip('#'))
            out.append('<h%d>%s</h%d>' % (level, inline(line[level:].strip()), level))
        elif line.startswith('|'):
            flush()
            rows = []
            while i < len(lines) and lines[i].startswith('|'):
                cells = [c.strip() for c in lines[i].strip().strip('|').split('|')]
                if not all(re.fullmatch(r'-+', c) for c in cells):
                    rows.append(cells)
                i += 1
            i -= 1
            body = ''.join('<tr>' + ''.join('<td>' + inline(c) + '</td>' for c in r) + '</tr>' for r in rows if any(r))
            out.append('<table>' + body + '</table>')
        elif line.startswith('> '):
            if para or items:
                flush()
            quote.append(line[2:].strip())
        elif line.startswith('- '):
            if para or quote:
                flush()
            items.append([line[2:].strip()])
        elif line.startswith('  ') and items:
            items[-1].append(line.strip())
        else:
            if items or quote:
                flush()
            para.append(line.strip())
        i += 1
    flush()
    return '\n'.join(out)


def main():
    text = open(SRC, encoding='utf-8').read().replace('\r\n', '\n')
    marker = '\n## Description\n'
    body = text.split(marker, 1)[1]
    page = ('<!doctype html>\n<html><head><meta charset="utf-8"><title>Project Ascension: page text</title>\n'
            '<style>body{font-family:sans-serif;max-width:46rem;margin:2rem auto;padding:0 1rem;line-height:1.5}'
            'td{padding:.2rem .8rem;vertical-align:top}</style></head><body>\n' + convert(body.split('\n')) + '\n</body></html>\n')
    with open(OUT, 'w', encoding='utf-8', newline='\n') as f:
        f.write(page)
    print('wrote pack/launch/curseforge-description.html')


if __name__ == '__main__':
    main()
