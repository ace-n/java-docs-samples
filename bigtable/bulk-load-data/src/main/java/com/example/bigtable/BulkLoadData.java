package com.example.bigtable;

import com.google.cloud.bigtable.batch.common.CloudBigtableService;
import com.google.cloud.bigtable.beam.CloudBigtableConfiguration;
import com.google.cloud.bigtable.beam.CloudBigtableConfiguration.Builder;
import com.google.cloud.bigtable.beam.CloudBigtableIO;
import com.google.cloud.bigtable.beam.CloudBigtableTableConfiguration;
import java.time.Instant;
import org.apache.beam.runners.dataflow.options.DataflowPipelineOptions;
import org.apache.beam.sdk.Pipeline;
import org.apache.beam.sdk.coders.VarIntCoder;
import org.apache.beam.sdk.io.GenerateSequence;
import org.apache.beam.sdk.options.Default;
import org.apache.beam.sdk.options.Description;
import org.apache.beam.sdk.options.PipelineOptions;
import org.apache.beam.sdk.options.PipelineOptionsFactory;
import org.apache.beam.sdk.transforms.Create;
import org.apache.beam.sdk.transforms.DoFn;
import org.apache.beam.sdk.transforms.DoFn.Element;
import org.apache.beam.sdk.transforms.DoFn.OutputReceiver;
import org.apache.beam.sdk.transforms.DoFn.ProcessElement;
import org.apache.beam.sdk.transforms.ParDo;
import org.apache.beam.sdk.values.KV;
import org.apache.hadoop.hbase.client.Mutation;
import org.apache.hadoop.hbase.client.Put;
import org.apache.hadoop.hbase.util.Bytes;
import org.apache.log4j.BasicConfigurator;
import org.joda.time.Duration;

public class BulkLoadData {
  public static void main(String[] args) {

    BigtableOptions options =
        PipelineOptionsFactory.fromArgs(args).withValidation().as(BigtableOptions.class);
    Pipeline p = Pipeline.create(options);
    CloudBigtableTableConfiguration bigtableTableConfig =
        new CloudBigtableTableConfiguration.Builder()
            .withProjectId(options.getBigtableProjectId())
            .withInstanceId(options.getBigtableInstanceId())
            .withTableId(options.getBigtableTableId())
            .build();

    p.apply(GenerateSequence.from(0).to(1000000))
        .apply(
            ParDo.of(
                new DoFn<Long, Mutation>() {
                  @ProcessElement
                  public void processElement(@Element Long rowkey, OutputReceiver<Mutation> out) {
                    Put row = new Put(Bytes.toBytes("" + rowkey));
                    long timestamp = System.currentTimeMillis();
                    System.out.println("" + rowkey + ", " + timestamp);

                    row.addColumn(
                        Bytes.toBytes("cf"), Bytes.toBytes("C"), timestamp, Bytes.toBytes(rowkey));
                    out.output(row);
                  }
                }))
        .apply(CloudBigtableIO.writeToTable(bigtableTableConfig));

    p.run().waitUntilFinish();
  }

  public interface BigtableOptions extends DataflowPipelineOptions {
    @Description("The Bigtable project ID, this can be different than your Dataflow project")
    @Default.String("bigtable-project")
    String getBigtableProjectId();

    void setBigtableProjectId(String bigtableProjectId);

    @Description("The Bigtable instance ID")
    @Default.String("bigtable-instance")
    String getBigtableInstanceId();

    void setBigtableInstanceId(String bigtableInstanceId);

    @Description("The Bigtable table ID in the instance.")
    @Default.String("bigtable-table")
    String getBigtableTableId();

    void setBigtableTableId(String bigtableTableId);
  }
}
