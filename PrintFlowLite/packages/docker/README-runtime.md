# PrintFlowLite Docker Runtime - Hướng dẫn sử dụng

## Tổng quan

PrintFlowLite đã được chạy thành công trong Docker với cấu hình tùy chỉnh từ source code. Ứng dụng sử dụng:
- **Tomcat 9.0** với JDK 17
- **PostgreSQL 16** làm cơ sở dữ liệu
- **Multi-stage Docker build** để biên dịch từ source code

## Trạng thái hiện tại

Ứng dụng đang chạy:
- **PrintFlowLite Web Server**: http://localhost:8080
- **PostgreSQL Database**: localhost:5444

## Cách sử dụng

### 1. Kiểm tra trạng thái containers

```bash
cd packages/docker
docker compose -f docker-compose.runtime.yml ps
```

### 2. Xem logs

```bash
# Xem logs của PrintFlowLite
docker compose -f docker-compose.runtime.yml logs printflowlite

# Xem logs của PostgreSQL
docker compose -f docker-compose.runtime.yml logs postgres

# Xem logs theo thời gian thực
docker compose -f docker-compose.runtime.yml logs -f printflowlite
```

### 3. Truy cập ứng dụng

Mở trình duyệt và truy cập:
```
http://localhost:8080
```

### 4. Dừng ứng dụng

```bash
cd packages/docker
docker compose -f docker-compose.runtime.yml down
```

### 5. Khởi động lại ứng dụng

```bash
cd packages/docker
docker compose -f docker-compose.runtime.yml up -d
```

### 6. Xóa hoàn toàn (bao gồm volumes)

```bash
cd packages/docker
docker compose -f docker-compose.runtime.yml down -v
```

## Cấu hình

### Ports
- **8080**: HTTP (PrintFlowLite web server)
- **5444**: PostgreSQL database

### Environment Variables
Các biến môi trường được cấu hình trong `docker-compose.runtime.yml`:
- `TZ`: Europe/Amsterdam (timezone)
- `SPRING_DATASOURCE_URL`: JDBC URL cho PostgreSQL
- `SPRING_DATASOURCE_USERNAME`: printflowlite
- `SPRING_DATASOURCE_PASSWORD`: change-me-in-docker-env

### Volumes
- `printflowlite_data`: Dữ liệu ứng dụng
- `printflowlite_logs`: Logs
- `printflowlite_custom`: Cấu hình tùy chỉnh
- `printflowlite_database`: PostgreSQL database

## Xây dựng lại Docker image

Nếu bạn thay đổi source code và muốn rebuild:

```bash
cd packages/docker
docker compose -f docker-compose.runtime.yml build --no-cache
docker compose -f docker-compose.runtime.yml up -d
```

## Lưu ý

1. **Database Password**: Mật khẩu PostgreSQL hiện tại là `change-me-in-docker-env`. Bạn nên thay đổi nó trong file `docker-compose.runtime.yml` trước khi sử dụng trong production.

2. **HTTPS**: Hiện tại ứng dụng chỉ chạy HTTP trên port 8080. Để bật HTTPS, bạn cần cấu hình SSL certificate.

3. **Performance**: Docker image hiện tại không bao gồm các công cụ xử lý PDF (poppler-utils, imagemagick, v.v.) để tránh lỗi build. Nếu cần tính năng xử lý PDF, bạn có thể cài đặt thêm.

4. **Network**: Containers chạy trong mạng Docker riêng biệt `printflowlite_network` với subnet `172.21.0.0/16`.

## Troubleshooting

### Container không khởi động
```bash
docker compose -f docker-compose.runtime.yml logs
```

### Không thể truy cập web
Kiểm tra xem port 8080 có đang bị占用 bởi ứng dụng khác không:
```bash
netstat -ano | findstr :8080
```

### Database connection error
Kiểm tra PostgreSQL container:
```bash
docker compose -f docker-compose.runtime.yml logs postgres
```

### Rebuild sau khi thay đổi code
```bash
docker compose -f docker-compose.runtime.yml down
docker compose -f docker-compose.runtime.yml build --no-cache
docker compose -f docker-compose.runtime.yml up -d
```

## Files đã tạo/modify

1. **Dockerfile.runtime**: Dockerfile để build và chạy từ source code
2. **docker-compose.runtime.yml**: Cấu hình Docker Compose cho runtime

## Tiếp theo

Để cải thiện cấu hình này cho production:
1. Thêm SSL/HTTPS configuration
2. Cấu hình backup database
3. Thêm monitoring và logging
4. Cấu hình resource limits
5. Thêm health checks chi tiết hơn
