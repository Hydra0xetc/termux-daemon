#include <arpa/inet.h>
#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include <sys/socket.h>
#include <unistd.h>
#include <libgen.h>
#include <limits.h>
#include <errno.h>
#include <stdbool.h>

#define HOST "127.0.0.1"
#define PORT 6969

#define unused(s) (void)s

typedef void (*CommandHandler)(int argc, char **argv);

typedef struct {
  const char *name;
  CommandHandler handler;
} SubCommand;

typedef struct {
  const char *name;
  const SubCommand *subcmd;
  size_t count;
} Command;

static const Command *find_cmd(const Command *cmds, size_t count,
    const char *name) {
  for (size_t i = 0; i < count; i++) {
    if (strcmp(cmds[i].name, name) == 0) {
      return &cmds[i];
    }
  }

  return NULL;
}

static const SubCommand *find_subcmd(const Command *cmd,
    const char *name) {
  for (size_t i = 0; i < cmd->count; i++) {
    if (strcmp(cmd->subcmd[i].name, name) == 0) {
      return &cmd->subcmd[i];
    }
  }

  return NULL;
}

static void usage(const char *name, const Command *cmds,
    size_t count) {
  fprintf(stderr, "usage:\n");
  fprintf(stderr, "  %s open <path>\n", name);

  for (size_t i = 0; i < count; i++) {
    fprintf(stderr, "  %s %s [", name, cmds[i].name);

    for (size_t j = 0; j < cmds[i].count; j++) {
      fprintf(stderr, "%s", cmds[i].subcmd[j].name);
      if (j + 1 < cmds[i].count) {
        fputc('|', stderr);
      }
    }

    fprintf(stderr, "]\n");
  }

  exit(1);
}

static inline char *__get_fullpath(char *path) {
  char *fullpath = malloc(PATH_MAX);

  if (realpath(path, fullpath) == NULL) {
    fprintf(stderr, "failed to get path '%s': %s\n", path,
        strerror(errno));
    exit(1);
  }

  return fullpath;
}

static int connect_server(void) {
  int sock = socket(AF_INET, SOCK_STREAM, 0);

  if (sock < 0) {
    fprintf(stderr, "failed to create socket: %s\n", strerror(errno));
    exit(1);
  }

  struct sockaddr_in addr = {
    .sin_family = AF_INET,
    .sin_port = htons(PORT),
  };

  inet_pton(AF_INET, HOST, &addr.sin_addr);

  if (connect(sock, (struct sockaddr *) &addr, sizeof(addr)) < 0) {
    fprintf(stderr,
      "failed connect to '%s:%d' %s\n", HOST, PORT, strerror(errno)
    );
    close(sock);
    exit(1);
  }

  return sock;
}

static void clipboard_get(int argc, char **argv) {
  unused(argc);
  unused(argv);

  int sock = connect_server();

  write(sock, "clipboard get\n", 14);

  char buf[4096];
  ssize_t n;

  while ((n = read(sock, buf, sizeof(buf))) > 0) {
    write(STDOUT_FILENO, buf, n);
  }

  shutdown(sock, SHUT_WR);
  close(sock);
}

static void clipboard_set(int argc, char **argv) {
  int sock = connect_server();

  write(sock, "clipboard set\n", 14);

  if (argc > 2) {
    for (int i = 2; i < argc; i++) {
      if (i > 2) {
        write(sock, " ", 1);
      }

      write(sock, argv[i], strlen(argv[i]));
    }
  } else {
    char buf[4096];
    ssize_t n;

    while ((n = read(STDIN_FILENO, buf, sizeof(buf))) > 0) {
      write(sock, buf, n);
    }
  }

  shutdown(sock, SHUT_WR);
  close(sock);
}

// NOTE: for simplicity, explicitly stating the mime type is
// not or has not been implemented
static void do_open(int argc, char **argv) {
  if (argc != 3) {
    fprintf(stderr, "usage: %s open <path>\n", argv[0]);
    exit(1);
  }

  int sock = connect_server();

  char *fullpath = __get_fullpath(argv[2]);
  dprintf(sock, "open\n%s\n", fullpath);

  shutdown(sock, SHUT_WR);

  char buf[1024];
  ssize_t n;

  while ((n = read(sock, buf, sizeof(buf))) > 0) {
    write(STDOUT_FILENO, buf, n);
  }

  free(fullpath);
  close(sock);
}

static void music_play(int argc, char **argv) {
  if (argc != 3) {
    fprintf(stderr, "usage: music play <path>\n");
    exit(1);
  }

  int sock = connect_server();
  char *fullpath = __get_fullpath(argv[2]);

  dprintf(sock, "music play\n%s\n", fullpath);

  shutdown(sock, SHUT_WR);

  free(fullpath);
  close(sock);
}

static void music_stop(int argc, char **argv) {
  unused(argv);
  if (argc != 2) {
    fprintf(stderr, "usage: music stop\n");
    exit(1);
  }

  int sock = connect_server();

  dprintf(sock, "music stop\n");
  shutdown(sock, SHUT_WR);
  close(sock);
}

int main(int argc, char **argv) {

  static const SubCommand clipboard_subs[] = {
    { "get", clipboard_get },
    { "set", clipboard_set },
  };

  static const SubCommand music_subs[] = {
    { "play", music_play },
    { "stop", music_stop },
  };

  static const Command commands[] = {
    {
      .name = "clipboard",
      .subcmd = clipboard_subs,
      .count = 2,
    },
    {
      .name = "music",
      .subcmd = music_subs,
      .count = 2,
    }
  };

  size_t commands_len = sizeof(commands) / sizeof(commands[0]);

  const char *program_name = basename(argv[0]);

  if (argc < 2) {
    usage(program_name, commands, commands_len);
  }

  // NOTE: I'm not sure what service this thing is categorized as,
  // so for now I'll just separate it.
  if (strcmp(argv[1], "open") == 0) {
    do_open(argc, argv);
    return 0;
  }

  if (argc < 3) {
    usage(program_name, commands, commands_len);
  }

  const Command *cmd = find_cmd(
        commands,
        commands_len,
        argv[1]
        );

  if (cmd == NULL) {
    usage(program_name, commands, commands_len);
  }

  const SubCommand *sub = find_subcmd(cmd, argv[2]);

  if (sub == NULL) {
    usage(program_name, commands, commands_len);
  }

  sub->handler(argc - 1, argv + 1);

  return 0;
}
