/***
 *     KPU - KPF Patching Utility
 *     Copyright (C) 2025 atsb
 * <p>
 *     This program is free software: you can redistribute it and/or modify
 *     it under the terms of the GNU General Public License as published by
 *     the Free Software Foundation, either version 3 of the License, or
 *     (at your option) any later version.
 * <p>
 *     This program is distributed in the hope that it will be useful,
 *     but WITHOUT ANY WARRANTY; without even the implied warranty of
 *     MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *     GNU General Public License for more details.
 * <p>
 *     You should have received a copy of the GNU General Public License
 *     along with this program.  If not, see https://www.gnu.org/licenses.
 */

import java.io.*;
import java.nio.file.*;
import java.nio.file.attribute.FileTime;
import java.util.*;
import java.util.zip.*;

public final class KPU {
    public static final class Options {
        public boolean makeBackup = true;
        public boolean caseInsensitive = true;
    }

    public static void patch(Path kpfPath, Path modDir, Options opt) throws IOException {
        Objects.requireNonNull(kpfPath, "kpfPath");
        Objects.requireNonNull(modDir, "modDir");
        if (!Files.isRegularFile(kpfPath))
            throw new FileNotFoundException("KPF not found: " + kpfPath);
        if (!Files.isDirectory(modDir))
            throw new FileNotFoundException("Mod folder not found: " + modDir);

        // get files
        Map<String, Path> replacements = new TreeMap<>();
        try (var stream = Files.walk(modDir)) {
            stream.filter(Files::isRegularFile).forEach(p -> {
                Path rel = modDir.relativize(p);
                String zipPath = normalizeZipPath(rel.toString());
                String key = opt.caseInsensitive ? zipPath.toLowerCase(Locale.ROOT) : zipPath;
                replacements.put(key, p);
            });
        }

        Path tmp = Paths.get(kpfPath.toString() + ".tmp");
        Path bak = Paths.get(kpfPath.toString() + ".bak");

        // copy all but leave those that are replaced (backups)
        try (ZipFile in = new ZipFile(kpfPath.toFile());
             OutputStream fos = Files.newOutputStream(tmp,
                     StandardOpenOption.CREATE,
                     StandardOpenOption.TRUNCATE_EXISTING);
             ZipOutputStream out = new ZipOutputStream(new BufferedOutputStream(fos))) {

            Enumeration<? extends ZipEntry> en = in.entries();
            byte[] buf = new byte[64 * 1024];
            while (en.hasMoreElements()) {
                ZipEntry e = en.nextElement();
                if (e.isDirectory())
                    continue;
                String name = e.getName();
                String key = opt.caseInsensitive ? name.toLowerCase(Locale.ROOT) : name;
                if (replacements.containsKey(key))
                    continue;

                ZipEntry ne = new ZipEntry(name);
                // old timestamps will be preserved
                if (e.getLastModifiedTime() != null)
                    ne.setLastModifiedTime(e.getLastModifiedTime());
                out.putNextEntry(ne);
                try (InputStream is = in.getInputStream(e)) {
                    copy(is, out, buf);
                }
                out.closeEntry();
            }

            // add replacements, preserve case
            for (Map.Entry<String, Path> re : replacements.entrySet()) {
                String name = re.getKey();
                if (!opt.caseInsensitive)
                    name = re.getKey();

                String entryName = opt.caseInsensitive ? deriveOriginalName(modDir, re.getValue()) : name;

                ZipEntry ne = new ZipEntry(entryName);
                try {
                    FileTime ft = Files.getLastModifiedTime(re.getValue());
                    ne.setLastModifiedTime(ft);
                } catch (IOException ignored) {}
                out.putNextEntry(ne);
                try (InputStream is = Files.newInputStream(re.getValue())) {
                    copy(is, out, buf);
                }
                out.closeEntry();
            }
        } catch (IOException e) {
            try {
                Files.deleteIfExists(tmp);
            }
            catch (IOException ignore) {}
            throw e;
        }

        if (opt.makeBackup) {
            try {
                Files.deleteIfExists(bak);
            }
            catch (IOException ignore) {}
            Files.move(kpfPath, bak, StandardCopyOption.REPLACE_EXISTING);
        } else {
            Files.deleteIfExists(kpfPath);
        }
        Files.move(tmp, kpfPath, StandardCopyOption.REPLACE_EXISTING);
    }

    private static void copy(InputStream in, OutputStream out, byte[] buf) throws IOException {
        int r;
        while ((r = in.read(buf)) != -1)
            out.write(buf, 0, r);
    }

    private static String normalizeZipPath(String p) {
        String s = p.replace('\\', '/');
        while (s.startsWith("./"))
            s = s.substring(2);
        while (s.startsWith("/"))
            s = s.substring(1);
        return s;
    }

    private static String deriveOriginalName(Path modRoot, Path file) {
        String rel = modRoot.relativize(file).toString();
        return normalizeZipPath(rel);
    }

    private KPU() {}
}
