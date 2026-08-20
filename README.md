# LapTech RAG Chatbot với Spring AI

Ứng dụng xây dựng pipeline RAG để nạp tài liệu thông tin cửa hàng LapTech vào PostgreSQL/pgvector và cung cấp API chatbot chỉ trả lời dựa trên tài liệu đã nạp.

## Chức năng đã hoàn thiện

- Đọc `LapTech_Store_Info.pdf` bằng `TikaDocumentReader`.
- Chia văn bản theo cửa sổ token bằng tokenizer `CL100K_BASE`: 350 token/chunk, overlap 60 token.
- Sinh embedding và lưu chunk vào `PgVectorStore`.
- Chống nạp trùng bằng SHA-256 của nội dung tài liệu và bảng `rag_ingested_document`.
- Endpoint riêng để ingest tài liệu cố định trong `resources`.
- Chat RAG bằng `ChatClient`, `QuestionAnswerAdvisor` và system prompt chống hallucination.
- Ghi nhớ tối đa 20 message gần nhất theo từng `sessionId` bằng `MessageWindowChatMemory`.
- Validation đầu vào và phản hồi lỗi JSON thống nhất.
- Không chứa API key hoặc thông tin Supabase thật trong source code.

## Kiến trúc xử lý

```text
PDF -> TikaDocumentReader -> Token window (350/60) -> OpenAI Embedding
                                                    -> PostgreSQL + pgvector

POST /api/v1/chat -> Chat Memory -> Vector similarity search -> OpenAI Chat -> answer
```

## Lý do chọn chiến lược chunking

Mỗi chunk có tối đa **350 token**. Kích thước này đủ chứa trọn một đoạn chính sách ngắn như đổi trả, bảo hành hoặc giao hàng, nhưng vẫn đủ nhỏ để truy vấn vector tập trung. Hai chunk liên tiếp chồng lấn **60 token** để giữ lại câu tiêu đề hoặc điều kiện nằm sát ranh giới chunk. Việc tách dùng tokenizer thay vì ước lượng theo số ký tự, nên kích thước và overlap là số token thực.

## Yêu cầu môi trường

- Java 21
- Docker Desktop nếu chạy PostgreSQL local; hoặc một PostgreSQL đã bật extension `vector` (ví dụ Supabase)
- OpenAI API key

## Chạy nhanh bằng PostgreSQL local

1. Tạo tệp cấu hình cá nhân:

   ```powershell
   Copy-Item .env.example .env
   ```

2. Mở `.env`, thay `OPENAI_API_KEY` bằng key thật. Không commit tệp này; `.gitignore` đã loại trừ `.env`.

   Tài khoản API phải có credits/quota khả dụng. Gói ChatGPT và hạn mức API được quản lý riêng; nếu OpenAI trả HTTP `429`, hãy kiểm tra Billing trên OpenAI API Platform.

3. Khởi động PostgreSQL/pgvector:

   ```powershell
   docker compose up -d
   ```

4. Chạy ứng dụng:

   ```powershell
   .\gradlew.bat bootRun
   ```

Ứng dụng tự tạo extension/bảng vector của Spring AI và bảng theo dõi ingest khi gọi endpoint lần đầu.

## Dùng Supabase

Thay ba biến database trong `.env` bằng thông tin của project Supabase. JDBC URL nên có dạng:

```properties
DATABASE_URL=jdbc:postgresql://<host>:5432/postgres?sslmode=require
DATABASE_USERNAME=<username>
DATABASE_PASSWORD=<password>
```

Trước khi chạy, bật extension `vector` trong Supabase. Model embedding mặc định là `text-embedding-3-small` với 1536 chiều; nếu đổi model phải đổi `EMBEDDING_DIMENSIONS` cho khớp và tạo lại bảng vector.

## Gọi API

### 1. Nạp dữ liệu

```powershell
Invoke-RestMethod -Method Post `
  -Uri http://localhost:8080/api/v1/admin/ingest-store-info
```

Lần đầu trả HTTP `201` và trạng thái `INGESTED`:

```json
{
  "status": "INGESTED",
  "source": "LapTech_Store_Info.pdf",
  "contentHash": "...",
  "chunkCount": 4
}
```

Gọi lại cùng tài liệu trả HTTP `200`, trạng thái `ALREADY_INGESTED` và không tạo thêm vector.

### 2. Chat

```powershell
$body = @{
  sessionId = "demo-001"
  message = "Cửa hàng có chi nhánh ở Hà Nội không?"
} | ConvertTo-Json

Invoke-RestMethod -Method Post `
  -Uri http://localhost:8080/api/v1/chat `
  -ContentType "application/json" `
  -Body $body
```

Một số câu hỏi kiểm thử:

- `Chính sách đổi trả như thế nào?`
- `Mua laptop trả góp được không?`
- `Laptop bảo hành bao lâu?` rồi hỏi tiếp cùng `sessionId`: `Còn pin thì sao?`
- `Thời tiết hôm nay thế nào?` - chatbot phải báo tài liệu chưa có thông tin, không tự suy đoán.

Memory hiện được lưu trong RAM, tách biệt bằng `sessionId`, và sẽ mất khi ứng dụng khởi động lại. Đây là lựa chọn phù hợp cho phạm vi bài tập; khi triển khai nhiều instance nên thay bằng JDBC/Redis chat-memory repository.

## Kiểm thử

```powershell
.\gradlew.bat test
```

Bộ test kiểm tra chunk token, metadata, luồng ingest thành công, cơ chế bỏ qua tài liệu trùng và validation request. Test không cần API key hay database thật.

## Cấu trúc chính

```text
src/main/java/.../
  chat/       API và dịch vụ chatbot
  config/     cấu hình RAG, memory và advisors
  ingestion/  đọc PDF, chunking, ingest và chống trùng
  web/        định dạng lỗi API
src/main/resources/
  docs/LapTech_Store_Info.pdf
  application.yml
```

## Lưu ý triển khai

Endpoint ingest nằm dưới `/admin` nhưng bài tập chưa tích hợp xác thực. Khi đưa lên môi trường thật cần bảo vệ endpoint bằng Spring Security hoặc giới hạn ở mạng nội bộ. Không ghi prompt, API key hay mật khẩu database vào log.
