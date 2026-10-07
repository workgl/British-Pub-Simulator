def rw(p, f):
    s = open(p, encoding='utf-8').read(); s2 = f(s)
    if s2 == s: print("NO CHANGE", p)
    open(p, 'w', encoding='utf-8', newline='').write(s2)
rw('src/pub/Save.java', lambda s: s.replace('    public static boolean save(int slot) {\n        if (Sim.S == null) return false;', '    public static boolean disabled;\n\n    public static boolean save(int slot) {\n        if (Sim.S == null || disabled) return false;'))
rw('src/pub/Main.java', lambda s: s.replace('        final String shotF = shot,', '        if (shot != null || soak > 0) Save.disabled = true;\n        final String shotF = shot,'))
rw('src/pub/Soak.java', lambda s: s.replace('Save.save(9); System.out.println("save ok: " + Save.load(9)', 'Save.disabled = false; Save.save(9); System.out.println("save ok: " + Save.load(9)').replace('System.exit(errors > 0 ? 1 : 0);', 'try { java.nio.file.Files.deleteIfExists(Save.file(9)); } catch (Exception e) { }\n        System.exit(errors > 0 ? 1 : 0);'))
