# termux-daemon

termux-daemon is a server that provides BLAZINGLY FAST handlers for
Termux services such as clipboard management and opening files with
external applications.

## Usage
```sh
# assume you already build this project
./termux-daemon
# then open a new terminal, and run
./src/cpp/android-daemon [get|set|open]
```
or you can put this command to your bashrc to automatically start
the server in the background
```bash
start_clipboard_daemon() {
  local PORT=6969
  local LOG="$PREFIX/var/log/termux-daemon.log"

  # check if the port already used
  nc -z 127.0.0.1 "$PORT" 2>/dev/null &&
    return

  "$PATH_TO_THIS_PROJECT/termux-daemon" \
    --port "$PORT" \
    &>>"$LOG" 2>&1 &
}

start_clipboard_daemon
```

## Comparison

### my clipboard manager

```console
$ time android-daemon set $(cat src/java/org/clipboard/ClipboardModule.java)

real	0m0.060s
user	0m0.040s
sys	0m0.000s
$ time android-daemon get >/dev/null

real	0m0.040s
user	0m0.020s
sys	0m0.000s
```
### termux clipboard manager
```
$ time termux-clipboard-set $(cat src/java/org/clipboard/ClipboardModule.java)

real	0m2.449s
user	0m0.000s
sys	0m0.040s
$ time termux-clipboard-get >/dev/null

real	0m0.063s
user	0m0.010s
sys	0m0.010s
```

### my content resolver

Cheats a little bit—it's actually still using Termux's provider/resolver.
but still BLAZINGLY FAST
See [ContentResolverModule.java](./src/java/org/termux/daemon/ContentResolverModule.java#L93)
```
$ time android-daemon open termux-daemon

real    0m0.047s
user    0m0.010s
sys     0m0.000s
```
### termux content resolver
```
$ time termux-open termux-daemon

real    0m2.003s
user    0m0.840s
sys     0m0.340s
```
