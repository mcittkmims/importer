# Two-Class Export Architecture

## Overview

The export functionality has been refactored into two separate classes following the Single Responsibility Principle:

### 1. `PartitionManager`

**Location**: `com.internship.importer.infrastructure.export.PartitionManager`

**Responsibilities**:

- Adds partitions for any new unpartitioned data from the staging table
- Never deletes existing partitions from `company_partition` table
- Always checks for new data and creates partitions as needed

**Key Methods**:

- `addPartitionsForNewData(int batchSize)` - Main entry point for adding new partitions
- `addPartitionsForNewData()` - Uses default batch size of 100
- `getPartitionInfo()` - Returns partition status information

**Key Features**:

- **Never deletes data**: Existing partitions in `company_partition` are never removed
- **Incremental approach**: Only creates partitions for data that hasn't been partitioned yet
- **Simple workflow**: Just one job - add partitions for new data

### 2. `HttpDataExporter`

**Location**: `com.internship.importer.infrastructure.export.HttpDataExporter`

**Responsibilities**:

- Processes existing partitions with multiple threads
- Retrieves batches from `company_partition` table
- Fetches actual data from staging table for each batch
- Builds and sends HTTP requests
- Marks individual records as exported in staging table
- Marks partition batches as completed in `company_partition` table

**Key Methods**:

- `sendStagingData()` - Main entry point for data export
- `processAllPartitions()` - Manages optimized multi-threaded partition processing with batch fetching
- `submitPartitionProcessing()` - Handles individual partition batch processing

**Key Features**:

- **Optimized batch fetching**: Fetches multiple partition batches at once to minimize database calls
- **Thread-safe processing**: Uses `FOR UPDATE SKIP LOCKED` for concurrent partition access
- **Simplified workflow**: Only tracks partition completion status, not individual record export status
- **Efficient polling**: No longer polls for `hasUnprocessedBatches()` on every iteration

## Workflow

### Step 1: Partition Management (PartitionManager)

```java
// PartitionManager adds partitions for any new unpartitioned data
partitionManager.addPartitionsForNewData();
```

**What it does**:

- Finds the highest `end_id` from existing partitions in `company_partition`
- Checks staging table for any unprocessed data with IDs higher than that
- Creates new partition records only for the new data
- Never deletes or modifies existing partitions

### Step 2: Multi-threaded Processing (HttpDataExporter)

```java
// HttpDataExporter processes the partitions with multiple threads
while (partitionRepository.hasUnprocessedBatches()) {
    Optional<PartitionBatch> batch = partitionRepository.getNextUnprocessedBatch();
    if (batch.isPresent()) {
        // Submit batch for processing in a separate thread
        futures.add(submitPartitionProcessing(batch.get(), ...));
    }
}
```

### Step 3: Individual Batch Processing

For each batch:

1. Get data from staging table using partition's start_id and end_id
2. Build HTTP multipart entity with the data
3. Send HTTP request
4. Mark partition batch as completed in `company_partition` table

## Key Improvements: Partition-Only Processing

### Simplified Data Tracking

The system now uses **partition-level tracking only** for export processing:

- **Partitioned data**: Records assigned to a partition (`partitioned = true`)
- **Completed partitions**: Partitions that have been successfully processed (`status = true` in `company_partition`)

### Updated Workflow

1. **PartitionManager** checks for `partitioned = false` records
2. Creates new partitions only for unpartitioned data beyond existing partition range
3. Marks records as `partitioned = true` when they get assigned to partitions
4. **HttpDataExporter** processes partitions by:
   - Getting unprocessed batches (`status = false`) from `company_partition`
   - Fetching ALL data in the partition range from staging table (regardless of individual export status)
   - Sending HTTP requests with the batch data
   - Marking partition as completed (`status = true`) in `company_partition`

### Key Benefits

- **Single source of truth**: Partition processing controlled solely by `company_partition.status`
- **Simplified logic**: No need to track individual record export status during processing
- **Atomic operations**: Either a whole partition is processed or it isn't
- **Better recovery**: Failed partitions remain unprocessed and can be retried
- **Cleaner separation**: `partitioned` field for assignment, `status` field for completion

This separation provides:

- **Clear partition logic**: Only unpartitioned data (`partitioned = false`) is considered for new partitions
- **Better workflow tracking**: Can track partition assignment separately from export completion
- **Incremental partitioning**: Only creates partitions for truly new, unpartitioned data

## Database Schema

### Staging table (e.g., japan_company_data)

```sql
CREATE TABLE IF NOT EXISTS japan_company_data (
    id BIGSERIAL PRIMARY KEY,
    raw_json JSONB,
    inserted_at TIMESTAMP DEFAULT NOW(),
    exported BOOL DEFAULT FALSE,       -- Tracks if record has been successfully exported
    partitioned BOOL DEFAULT FALSE     -- Tracks if record has been assigned to a partition
);
```

### company_partition table

```sql
CREATE TABLE IF NOT EXISTS company_partition (
    id BIGSERIAL PRIMARY KEY,
    start_id INT,
    end_id INT,
    status BOOLEAN
);
```

### Thread Safety

- Uses `FOR UPDATE SKIP LOCKED` to safely retrieve partition batches
- Multiple threads can work concurrently without conflicts
- Failed batches remain unprocessed and can be picked up by other threads

## Benefits

### 1. **Separation of Concerns**

- Partition management is isolated from data processing
- Each class has a single, clear responsibility

### 2. **Better Testability**

- Can test partition logic independently of HTTP processing
- Can test HTTP processing with pre-created partitions

### 3. **Improved Maintainability**

- Changes to partition logic don't affect HTTP processing
- Easier to understand and modify individual components

### 4. **Scalability**

- Partition creation is done once upfront
- Multiple threads efficiently process partitions concurrently
- Easy to adjust batch sizes and thread counts

### 5. **Fault Tolerance**

- If a thread fails, the partition remains unprocessed
- Can retry failed partitions without affecting successful ones
- Clear tracking of processing status

## Usage Example

```java
// Create the components
PartitionManager partitionManager = new PartitionManager(partitionRepository);
HttpDataExporter exporter = new HttpDataExporter(
    uploadUrl, stagingRepository, partitionRepository,
    executorService, repositoryHelper, partitionManager
);

// The workflow is handled automatically by the exporter
exporter.sendStagingData(companyJson, industryJson, taxAuthorityJson, taxInfoJson);

// Or use PartitionManager directly to just add partitions for new data
partitionManager.addPartitionsForNewData(); // Uses default batch size of 100
partitionManager.addPartitionsForNewData(50); // Custom batch size
```

## Configuration

- Default batch size: 100 records per partition
- Thread pool size: Configurable via ExecutorService
- Concurrent task limit: 2x available processors
