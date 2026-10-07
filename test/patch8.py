def rw(p, f):
    s = open(p, encoding='utf-8').read(); s2 = f(s)
    if s2 == s: print("NO CHANGE", p)
    open(p, 'w', encoding='utf-8', newline='').write(s2)
rw('src/pub/UI.java', lambda s: s.replace('public static JPanel vbox() { JPanel p = new JPanel(); p.setLayout', 'public static JPanel vbox() { JPanel p = new JPanel() { @Override public Component add(Component c) { if (c instanceof JComponent j) j.setAlignmentX(LEFT_ALIGNMENT); return super.add(c); } }; p.setLayout'))
rw('src/pub/GameWindow.java', lambda s: s.replace("width:520px;font-family:Georgia;font-size:14px", "width:470px;font-family:Georgia;font-size:14px"))
