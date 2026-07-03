#include <arpa/inet.h>
#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include <sys/socket.h>
#include <unistd.h>
#include <libgen.h>
#include <limits.h>
#include <errno.h>

#define HOST "127.0.0.1"
#define PORT 6969

static void usage(const char *name) {
  fprintf(stderr, "usage:\n");
  fprintf(stderr, "  %s get\n", name);
  fprintf(stderr, "  %s set [text]\n", name);
  fprintf(stderr, "  %s open [path] [mimi]\n", name);
  exit(1);
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

static void do_get(void) {
  int sock = connect_server();

  write(sock, "get\n", 4);

  char buf[4096];
  ssize_t n;

  while ((n = read(sock, buf, sizeof(buf))) > 0) {
    write(STDOUT_FILENO, buf, n);
  }

  close(sock);
}

static void do_set(int argc, char **argv) {
  int sock = connect_server();

  write(sock, "set\n", 4);

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

  char fullpath[PATH_MAX];

  if (realpath(argv[2], fullpath) == NULL) {
    fprintf(stderr, "failed to get path '%s': %s\n", argv[2],
        strerror(errno));
    exit(1);
  }

  int sock = connect_server();

  dprintf(sock, "open\n%s\n", fullpath);

  shutdown(sock, SHUT_WR);

  char buf[1024];
  ssize_t n;

  while ((n = read(sock, buf, sizeof(buf))) > 0) {
    write(STDOUT_FILENO, buf, n);
  }

  close(sock);
}

int main(int argc, char **argv) {
  const char *program_name = basename(argv[0]);
  if (argc < 2) {
    usage(program_name);
  }

  if (strcmp(argv[1], "get") == 0) {
    do_get();
  } else if (strcmp(argv[1], "set") == 0) {
    do_set(argc, argv);
  } else if (strcmp(argv[1], "open") == 0) {
    do_open(argc, argv);
  } else {
    usage(program_name);
  }

  return 0;
}
