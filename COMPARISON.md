## my clipboard manager
```
$ time android-daemon set $(cat src/java/org/clipboard/ClipboardModule.java)

real	0m0.060s
user	0m0.040s
sys	0m0.000s
$ time android-daemon get >/dev/null

real	0m0.040s
user	0m0.020s
sys	0m0.000s
```
## termux clipboard manager
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

## my content resolver (actually still using termux resolver)
```
$ time android-daemon open termux-daemon

real    0m0.047s
user    0m0.010s
sys     0m0.000s
```
## termux content resolver (always executed am broadcast)
```
$ time termux-open termux-daemon

real    0m2.003s
user    0m0.840s
sys     0m0.340s
```
