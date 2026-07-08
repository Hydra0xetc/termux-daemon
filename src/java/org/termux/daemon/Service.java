package org.termux.daemon;

import java.util.ArrayList;
import java.util.List;

import java.io.File;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.FileReader;
import java.io.BufferedReader;
import java.io.IOException;

public class Service {
  String name;
  List<ServiceCmd> cmd = new ArrayList<>();

  Service(String name) {
    this.name = name;
  }

  ServiceCmd findCmd(String cmdName) {
    for (ServiceCmd c : cmd) {
      if (c.name.equals(cmdName)) return c;
    }
    return null;
  }

  @FunctionalInterface
  public interface Handler {
    void handle(InputStream in, OutputStream out, String client)
        throws Exception;
  }

  public static class ServiceCmd {
    String name;
    Handler handler;

    ServiceCmd(String name) {
      this.name = name;
    }
  }

  public static List<Service> parseService(File file) throws IOException {
    StringBuilder sb = new StringBuilder();
    try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
      String line;
      while ((line = reader.readLine()) != null) {
        sb.append(line).append("\n");
      }
    }
    return parseServiceFromString(sb.toString());
  }

  private static List<Service> parseServiceFromString(String src) {
    List<Service> services = new ArrayList<>();
    String[] lines = src.split("\n+");

    for (String line : lines) {
      if (line.isEmpty()) continue;

      int colonIdx = line.indexOf(':');
      if (colonIdx == -1) continue;

      String name = line.substring(0, colonIdx);
      String rest = line.substring(colonIdx + 1);

      Service newService = new Service(name);
      services.add(newService);

      if (!rest.isEmpty()) {
        String[] cmds = rest.split(",+");
        for (String cmd : cmds) {
          if (cmd.isEmpty()) continue;
          newService.cmd.add(new ServiceCmd(cmd));
        }
      }
    }

    return services;
  }

  public static void registerHandler(List<Service> services,
      String serviceName, String cmdName, Handler handler) {
    for (Service s : services) {
      if (s.name.equals(serviceName)) {
        ServiceCmd c = s.findCmd(cmdName);
        if (c != null) {
          c.handler = handler;
        }
      }
    }
  }

}
