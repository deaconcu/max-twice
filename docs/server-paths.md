# 服务器路径清单

> 服务器：Ubuntu / Debian  
> 更新时间：2026-04-16

---

## Java 应用 (twicemax)

| 类型 | 路径 |
|------|------|
| Jar 包 | `/opt/twicemax.jar` |
| 环境变量 | `/etc/twicemax.env` |
| Systemd 服务 | `/etc/systemd/system/twicemax-service.service` |
| 应用日志 | `/var/log/twicemax/app.log` |

```bash
systemctl status twicemax-service
systemctl restart twicemax-service
tail -f /var/log/twicemax/app.log
journalctl -u twicemax-service -f
```

---

## MySQL

| 类型 | 路径 |
|------|------|
| 数据目录 | `/var/lib/mysql/` |
| 配置文件 | `/etc/mysql/mysql.conf.d/mysqld.cnf` |
| 日志 | `/var/log/mysql/error.log` |

```bash
systemctl status mysql
systemctl restart mysql
mysql -u twicemax -p
tail -f /var/log/mysql/error.log
```

---

## Redis

| 类型 | 路径 |
|------|------|
| 数据目录 | `/var/lib/redis/` |
| 配置文件 | `/etc/redis/redis.conf` |
| 日志 | `/var/log/redis/redis-server.log` |

```bash
systemctl status redis
systemctl restart redis
redis-cli ping
tail -f /var/log/redis/redis-server.log
```

---

## Meilisearch

| 类型 | 路径 |
|------|------|
| 二进制 | `/usr/local/bin/meilisearch` |
| 数据目录 | `/var/lib/meilisearch/` |
| Systemd 服务 | `/etc/systemd/system/meilisearch.service` |

```bash
systemctl status meilisearch
systemctl restart meilisearch
journalctl -u meilisearch -f
```

---

## Milvus

| 类型 | 路径 |
|------|------|
| Docker Compose | `/opt/milvus/docker-compose.yml` |
| 数据目录 | `/opt/milvus/volumes/` |

```bash
cd /opt/milvus && docker compose ps
cd /opt/milvus && docker compose up -d
cd /opt/milvus && docker compose down
cd /opt/milvus && docker compose logs -f
```

---

## Nginx

| 类型 | 路径 |
|------|------|
| 主配置 | `/etc/nginx/nginx.conf` |
| 站点配置 | `/etc/nginx/sites-available/api.maxtwice.com` |
| 已启用站点 | `/etc/nginx/sites-enabled/` |
| 访问日志 | `/var/log/nginx/access.log` |
| 错误日志 | `/var/log/nginx/error.log` |

```bash
systemctl status nginx
systemctl reload nginx
nginx -t
tail -f /var/log/nginx/access.log
tail -f /var/log/nginx/error.log
```
