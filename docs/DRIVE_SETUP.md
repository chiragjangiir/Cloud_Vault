# External drive setup

Cloud Vault never formats, partitions or mounts anything. **You** mount the
drive, **you** create the directory, **you** register it in the admin UI.
The application only ever receives an explicit path and verifies it before
using it (§4).

## 1. Mount the drive (OS-specific)

### Linux

```bash
# identify the device (example: /dev/sdb1)
lsblk -f

# create a mount point
sudo mkdir -p /mnt/cloudvault-storage

# mount (ext4 example; use ntfs3/exfat for drives shared with Windows)
sudo mount /dev/sdb1 /mnt/cloudvault-storage

# make it permanent
echo '/dev/sdb1 /mnt/cloudvault-storage ext4 defaults,nofail 0 2' | sudo tee -a /etc/fstab
```

### macOS

```bash
# list volumes
diskutil list

# mount point (APFS/exFAT volumes normally auto-mount under /Volumes)
diskutil mountDisk disk2

# for a stable, explicit path, create a directory on the volume and use it:
sudo mkdir -p /Volumes/CloudVaultStorage
```

### Windows (WSL2 or native path)

```powershell
# drives arrive as D:\, E:\, … — pick a dedicated folder
mkdir D:\cloudvault-storage
```

## 2. Create the storage directory and permissions

The application must be able to read/write it **without root** (§51):

```bash
# Linux/macOS — chown to the user that runs Cloud Vault
sudo mkdir -p /mnt/cloudvault-storage
sudo chown "$USER":"$USER" /mnt/cloudvault-storage
chmod 750 /mnt/cloudvault-storage
```

Run the service as that same unprivileged user; Cloud Vault should never need
`sudo`.

## 3. Point the application at it

```bash
export STORAGE_ROOT=/mnt/cloudvault-storage
# Docker: put the path in .env as STORAGE_HOST_PATH=/mnt/cloudvault-storage
```

On startup the bootstrap registers the default location from `STORAGE_ROOT`,
verifies it (exists, directory, readable, writable, capacity) and reports the
**real** capacity/filesystem in **Admin → Storage**.

## 4. Register additional locations

**Admin → Storage Locations → Add location** (or
`POST /api/v1/admin/storage/locations`):

```json
{ "name": "HDD-01", "path": "/mnt/cloudvault-archive", "minFreeBytes": 10737418240 }
```

Cloud Vault validates the path before accepting it. Arbitrary paths are
**never** accepted from normal users — this endpoint is admin-only.

## 5. Disconnect / reconnect behaviour

- While the drive is disconnected, the health check flips the location to
  `OFFLINE`; reads/writes on objects living there fail with
  *storage temporarily unavailable* (no pretending).
- Reconnect the drive, then trigger **Run health check** (admin UI) or
  `POST /api/v1/admin/storage/locations/{id}/health`; a passing probe returns
  the location to `ONLINE`.
- Never delete a location that still holds files — **migrate** them first
  (Admin → Storage → Migrate: preview → confirm → progress → verify).

## 6. Migration safety

Migration copies → checksums → switches metadata → deletes the source. If
anything fails the source remains intact. A second drive is **not** a backup —
see [BACKUP.md](BACKUP.md).
