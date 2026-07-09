#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include <errno.h>
#include <limits.h>
#include <unistd.h>
#include <libgen.h>
#include <sys/socket.h>
#include <arpa/inet.h>
#include "config.h"

#define DEFAULT_BUF_SIZE 1024
#define UNUSED(s) (void)(s)

typedef void (*ServiceHandler)(int argc, char **argv);
typedef struct {
  const char *name;
  ServiceHandler handler;
} ServiceCmd;

typedef struct {
  ServiceCmd *items;
  size_t count;
  size_t capacity;
} ServiceCmds;

typedef struct {
  const char *name;
  ServiceCmds cmd;
} Service;

typedef struct {
  Service *items;
  size_t count;
  size_t capacity;
} Services;

#define arr_push(arr, type, value)                             \
  do {                                                         \
    if ((arr)->count == (arr)->capacity) {                     \
      size_t cap = (arr)->capacity ? (arr)->capacity * 2 : 4;  \
      void *tmp = realloc((arr)->items, cap * sizeof(type));   \
      if (!tmp) {                                              \
        perror("realloc");                                     \
        exit(EXIT_FAILURE);                                    \
      }                                                        \
      (arr)->items = tmp;                                      \
      (arr)->capacity = cap;                                   \
    }                                                          \
    (arr)->items[(arr)->count++] = (value);                    \
  } while (0)

#define arr_free(arr)         \
  do {                        \
    free((arr)->items);       \
    (arr)->items = NULL;      \
    (arr)->count = 0;         \
    (arr)->capacity = 0;      \
  } while (0)

Services parse_service(char *src) {
  Services services = {0};

  char *save_line;
  char *line = strtok_r(src, "\n", &save_line);

  while (line) {
    char *colon = strchr(line, ':');
    if (!colon) {
      line = strtok_r(NULL, "\n", &save_line);
      continue;
    }

    *colon = '\0';

    Service new_service = { .name = line, .cmd = {0} };
    arr_push(&services, Service, new_service);

    Service *curr = &services.items[services.count - 1];

    char *save_cmd;
    char *cmd = strtok_r(colon + 1, ",", &save_cmd);

    while (cmd) {
      ServiceCmd new_cmd = { .name = cmd, .handler = NULL };
      arr_push(&curr->cmd, ServiceCmd, new_cmd);
      cmd = strtok_r(NULL, ",", &save_cmd);
    }

    line = strtok_r(NULL, "\n", &save_line);
  }

  return services;
}

static void bind_handler(Services *services, const char *service,
    const char *cmd, ServiceHandler handler) {
  for (size_t i = 0; i < services->count; i++) {
    if (strcmp(services->items[i].name, service) != 0) continue;

    for (size_t j = 0; j < services->items[i].cmd.count; j++) {
      if (!strcmp(services->items[i].cmd.items[j].name, cmd)) {
        services->items[i].cmd.items[j].handler = handler;
        return;
      }
    }
  }
  fprintf(stderr, "warning: no such command to bind: %s:%s\n", service, cmd);
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

static inline size_t __read_stdin(char *buf, int sock) {
  ssize_t n;
  while ((n = read(STDIN_FILENO, buf, sizeof(buf))) > 0) {
    write(sock, buf, n);
  }

  return n;
}

static inline size_t __read_stdout(char *buf, int sock) {
  ssize_t n;
  while ((n = read(sock, buf, sizeof(buf))) > 0) {
    write(STDOUT_FILENO, buf, n);
  }
  return n;
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
  UNUSED(argc);
  UNUSED(argv);

  int sock = connect_server();

  write(sock, "clipboard get\n", 14);

  char buf[DEFAULT_BUF_SIZE];
  __read_stdout(buf, sock);

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
    char buf[DEFAULT_BUF_SIZE];
    __read_stdin(buf, sock);
  }

  shutdown(sock, SHUT_WR);
  close(sock);
}

// NOTE: for simplicity, explicitly stating the mime type is
// not or has not been implemented
static void open_file(int argc, char **argv) {
  UNUSED(argv);

  if (argc != 3) {
    fprintf(stderr, "usage: open file <path>\n");
    exit(1);
  }

  int sock = connect_server();

  char *fullpath = __get_fullpath(argv[2]);
  dprintf(sock, "open file\n%s\n", fullpath);

  shutdown(sock, SHUT_WR);

  char buf[DEFAULT_BUF_SIZE];
  __read_stdout(buf, sock);

  free(fullpath);
  close(sock);
}

static void open_url(int argc, char **argv) {
  UNUSED(argv);

  if (argc != 3) {
    fprintf(stderr, "usage: open url <url>\n");
    exit(1);
  }

  int sock = connect_server();

  dprintf(sock, "open url\n%s\n", argv[2]);
  shutdown(sock, SHUT_WR);

  char buf[DEFAULT_BUF_SIZE];
  __read_stdout(buf, sock);

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

  // char buf[DEFAULT_BUF_SIZE];
  // __read_stdout(buf, sock);

  free(fullpath);
  close(sock);
}

static void music_pause(int argc, char **argv) {
  UNUSED(argv);
  UNUSED(argc);

  int sock = connect_server();

  dprintf(sock, "music pause\n");
  shutdown(sock, SHUT_WR);

  char buf[DEFAULT_BUF_SIZE];
  __read_stdout(buf, sock);

  close(sock);
}

static void music_resume(int argc, char **argv) {
  UNUSED(argv);
  UNUSED(argc);

  int sock = connect_server();
  dprintf(sock, "music resume\n");
  shutdown(sock, SHUT_WR);
  char buf[DEFAULT_BUF_SIZE];
  __read_stdout(buf, sock);
  close(sock);
}

static void music_stop(int argc, char **argv) {
  UNUSED(argv);
  UNUSED(argc);
  int sock = connect_server();

  dprintf(sock, "music stop\n");
  shutdown(sock, SHUT_WR);

  char buf[DEFAULT_BUF_SIZE];
  __read_stdout(buf, sock);

  close(sock);
}

static void apk_open(int argc, char **argv) {
  UNUSED(argv);

  if (argc != 3) {
    fprintf(stderr, "usage: apk open <name>\n");
    exit(EXIT_FAILURE);
  }

  int sock = connect_server();

  dprintf(sock, "apk open\n%s\n", argv[2]);
  shutdown(sock, SHUT_WR);

  char buf[DEFAULT_BUF_SIZE];
  __read_stdout(buf, sock);
  close(sock);
}

static void apk_list(int argc, char **argv) {
  UNUSED(argv);
  UNUSED(argc);

  int sock = connect_server();

  dprintf(sock, "apk list\n");
  shutdown(sock, SHUT_WR);

  char buf[DEFAULT_BUF_SIZE];
  __read_stdout(buf, sock);
  close(sock);
}

static void apk_scan(int argc, char **argv) {
  int sock = connect_server();

  dprintf(sock, "apk scan\n");
  shutdown(sock, SHUT_WR);

  char buf[DEFAULT_BUF_SIZE];
  __read_stdout(buf, sock);
  close(sock);
}

static void print_help(const char *program_name) {
  printf(
    "Usage: %s <services> [OPTIONS]\n"
    "\n"
    "options:\n"
    "   -h/--help             print this help message\n"
    "   -l/--list-service     list available services\n"
  , program_name);
}

static char *__get_service_path(void) {
    static char result[PATH_MAX];
    char exe_path[PATH_MAX];

    ssize_t len = readlink("/proc/self/exe", exe_path, sizeof(exe_path) - 1);
    if (len == -1) {
        result[0] = '\0';
        return result;
    }
    exe_path[len] = '\0';

    char *dir = dirname(exe_path);
    snprintf(result, sizeof(result), "%s/../share/termux-daemon/SERVICE", dir);
    return result;
}

int main(int argc, char **argv) {
  char buffer[2048];
  const char *service_path = __get_service_path();
  FILE *fp = fopen(service_path, "rb");
  if (!fp) {
    fprintf(stderr, "Error: failed to open file %s: %s\n",
        service_path, strerror(errno));
    return EXIT_FAILURE;
  }

  size_t n = fread(buffer, 1, sizeof(buffer) - 1, fp);
  if (ferror(fp)) {
    fprintf(stderr, "Error: failed to read file %s: %s\n",
        service_path, strerror(errno));
    fclose(fp);
    return EXIT_FAILURE;
  }
  buffer[n] = '\0';
  fclose(fp);

  Services services = parse_service(buffer);
  // TODO: better initialisation service handler
  bind_handler(&services, "clipboard", "get", clipboard_get);
  bind_handler(&services, "clipboard", "set", clipboard_set);
  bind_handler(&services, "music", "play", music_play);
  bind_handler(&services, "music", "pause", music_pause);
  bind_handler(&services, "music", "stop", music_stop);
  bind_handler(&services, "music", "resume", music_resume);
  bind_handler(&services, "open", "file", open_file);
  bind_handler(&services, "open", "url", open_url);
  bind_handler(&services, "apk", "open", apk_open);
  bind_handler(&services, "apk", "list", apk_list);
  bind_handler(&services, "apk", "scan", apk_scan);

  if (argc < 2) {
    print_help(PROGRAM_NAME);
    arr_free(&services);
    return EXIT_FAILURE;
  }

  for (int i = 0; i < argc; i++) {
    if (!strncmp(argv[i], "-l", 2) ||
        !strncmp(argv[i], "--list-service", 14)) {
      printf("Available services: \n");
      for (int j = 0; j < services.count; j++) {
        printf("    %s\n", services.items[j].name);
      }
      arr_free(&services);
      return EXIT_SUCCESS;
    } else if (!strncmp(argv[i], "-h", 2) ||
        !strncmp(argv[i], "--help", 6)) {
      print_help(PROGRAM_NAME);
      return EXIT_SUCCESS;
    }
  }

  Service *selected_service = NULL;
  ServiceCmd *selected_service_cmd = NULL;

  for (size_t i = 0; i < services.count; i++) {
    if (!strcmp(argv[1], services.items[i].name)) {
      selected_service = &services.items[i];
      break;
    }
  }

  if (!selected_service) {
    fprintf(stderr, "Unknown service: %s\n", argv[1]);
    arr_free(&services);
    return EXIT_FAILURE;
  }

  if (argc < 3) {
    fprintf(stderr, "Usage: %s [", selected_service->name);
    for (size_t h = 0; h < selected_service->cmd.count; h++) {
      printf("%s", selected_service->cmd.items[h].name);
      if (h + 1 < selected_service->cmd.count) {
        putchar('|');
      }
    }
    printf("]\n");
    arr_free(&services);
    return EXIT_FAILURE;
  }

  for (size_t j = 0; j < selected_service->cmd.count; j++) {
    if (!strcmp(argv[2], selected_service->cmd.items[j].name)) {
      selected_service_cmd = &selected_service->cmd.items[j];
      break;
    }
  }

  if (!selected_service_cmd) {
    fprintf(stderr, "Unknown command '%s' for service '%s'\n", argv[2], argv[1]);
    arr_free(&services);
    return EXIT_FAILURE;
  }

  if (selected_service_cmd->handler) {
    selected_service_cmd->handler(argc - 1, argv + 1);
  } else {
    printf("Command '%s' has no handler registered\n", selected_service_cmd->name);
  }

  for (size_t i = 0; i < services.count; i++)
    arr_free(&services.items[i].cmd);

  arr_free(&services);

  return 0;
}
