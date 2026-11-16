import pathlib
import subprocess
import sys

BASE = pathlib.Path(__file__).parent
JUNIT = BASE / "lib" / "junit-platform-console-standalone-1.11.4.jar"
MAIN_OUT = BASE / "build" / "classes"
TEST_OUT = BASE / "build" / "test-classes"


def javac(sources, out, classpath=None):
    out.mkdir(parents=True, exist_ok=True)
    argfile = BASE / "build" / ("argfile-" + out.name + ".txt")
    argfile.parent.mkdir(parents=True, exist_ok=True)
    argfile.write_text("\n".join('"%s"' % str(s).replace("\\", "/") for s in sources), encoding="utf-8")
    cmd = ["javac", "--release", "17", "-encoding", "UTF-8", "-Xlint:all"]
    if classpath:
        cmd += ["-cp", classpath]
    cmd += ["-d", str(out), "@" + str(argfile)]
    r = subprocess.run(cmd, capture_output=True, text=True)
    if r.stdout:
        print(r.stdout)
    if r.stderr:
        print(r.stderr)
    if r.returncode != 0:
        sys.exit(r.returncode)


def main():
    main_sources = sorted(pathlib.Path(BASE / "src/main/java").rglob("*.java"))
    print("Compilando %d clases de producción..." % len(main_sources))
    javac(main_sources, MAIN_OUT)

    test_sources = sorted(pathlib.Path(BASE / "src/test/java").rglob("*.java"))
    print("Compilando %d clases de prueba..." % len(test_sources))
    javac(test_sources, TEST_OUT, classpath=f"{MAIN_OUT};{JUNIT}")

    print("Ejecutando pruebas...\n")
    cmd = [
        "java", "-jar", str(JUNIT),
        "execute",
        "--class-path", f"{MAIN_OUT};{TEST_OUT}",
        "--scan-class-path", str(TEST_OUT),
        "--details", "tree",
        "--reports-dir", "build/reports",
        "--disable-ansi-colors",
    ]
    r = subprocess.run(cmd, capture_output=True, text=True, encoding="utf-8", errors="replace")
    (BASE / "build" / "resultado-pruebas.txt").write_text(r.stdout + r.stderr, encoding="utf-8")
    print(r.stdout.encode("ascii", "replace").decode("ascii"))
    if r.stderr:
        print(r.stderr.encode("ascii", "replace").decode("ascii"))
    sys.exit(r.returncode)


if __name__ == "__main__":
    main()
