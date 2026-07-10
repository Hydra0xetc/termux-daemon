# termux-daemon

termux-daemon is a server that provides BLAZINGLY FAST handlers for
Termux services such as clipboard management and opening files with
external applications.

## Usage
You can add this command to your bashrc to automatically start
the server in the background:
```bash
start_termux_daemon() {
  local PORT=6969
  local LOG="$PREFIX/var/log/termux-daemon.log"

  # check if the port is already in use
  nc -z 127.0.0.1 "$PORT" 2>/dev/null &&
    return

  setsid termux-daemon --port "$PORT" &>>"$LOG" 2>&1 &
}

start_termux_daemon
```
Once the server is running, you can call `android-daemon` with a service
and its subcommand. For example, to get the current system clipboard
content, run:
```sh
android-daemon clipboard get
```
Here, `clipboard` is the service and `get` is the subcommand.
Below is the list of available services:
```
clipboard [get|set]
music     [play|pause|stop|resume]
open      [file|url]
apk       [open|scan|list|uninstall]
```

## Idea
I noticed that Termux scripts always run through the `am` command, so I
looked into it a bit and found that it's actually android JVM (`app_process` or
`dalvikvm`) with a full initialization. This means Termux services are slow
simply because they initialize the Android JVM, which is heavy, and then let
it die right after each call. So the question became: what if we initialize
the Android JVM once and keep it alive instead? That's exactly what this project
does — it initializes the Android JVM (via `app_process`) a single time and
keeps it running.

## Integreting with another tools
### neovim
```lua
vim.g.clipboard = {
  name = "android-clipboard",
  copy = {
    ["+"] = { "android-daemon", "clipboard", "set" },
    ["*"] = { "android-daemon", "clipboard", "set" },
  },
  paste = {
    ["+"] = { "android-daemon", "clipboard", "get" },
    ["*"] = { "android-daemon", "clipboard", "get" },
  },
  cache_enabled = 0,
}

-- override vim.ui.open implementation
vim.ui.open = function(path, opts)
  local function startsWith(str, prefix)
    return string.sub(str, 1, #prefix) == prefix
  end

  if startsWith(path, "http://") or startsWith(path, "https://") then
    vim.fn.jobstart({ "android-daemon", "open", "url", path, }, {
      detach = true,
    })
  end

  vim.fn.jobstart({ "android-daemon", "open", "file", path, }, {
    detach = true,
  })

  return true
end
```

### tmux
```tmux
bind-key p run-shell "android-daemon clipboard get | tmux load-buffer - && tmux paste-buffer"
```

## Comparison

### My clipboard manager

```console
$ time android-daemon clipboard set $(cat src/java/org/clipboard/ClipboardModule.java)

real	0m0.060s
user	0m0.040s
sys	0m0.000s
$ time android-daemon clipboard get >/dev/null

real	0m0.040s
user	0m0.020s
sys	0m0.000s
```
### Termux clipboard manager
```console
$ time termux-clipboard-set $(cat src/java/org/clipboard/ClipboardModule.java)

real	0m2.449s
user	0m0.000s
sys	0m0.040s
$ time termux-clipboard-get >/dev/null

real	0m0.063s
user	0m0.010s
sys	0m0.010s
```

### My content resolver

Cheats a little bit — it's actually still using Termux's provider/resolver,
but it's still BLAZINGLY FAST.
See [ContentResolverModule.java](./src/java/org/termux/daemon/ContentResolverModule.java#L93)
```console
$ time android-daemon open file termux-daemon

real    0m0.047s
user    0m0.010s
sys     0m0.000s
```
### Termux content resolver
```console
$ time termux-open termux-daemon

real    0m2.003s
user    0m0.840s
sys     0m0.340s
```

## Target
Fully free from dependency on Termux's own services (e.g. `termux-api`),
so this daemon can work standalone without relying on Termux add-ons.

# License
[MIT](./LICENSE)
