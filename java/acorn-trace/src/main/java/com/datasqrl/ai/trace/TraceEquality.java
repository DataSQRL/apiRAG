package com.datasqrl.ai.trace;

import com.google.common.base.Preconditions;
import java.util.Collections;
import java.util.List;
import lombok.Builder;
import lombok.Singular;
import lombok.Value;
import org.apache.commons.collections.ListUtils;

@Value
@Builder
public class TraceEquality implements TraceComparisonResult {

  boolean equal;
  @Singular
  List<String> differences;

  @Override
  public boolean isCorrect() {
    return isEqual();
  }

  public String getMessage() {
    return String.join("\n", differences);
  }

  public TraceEquality combine(TraceEquality other) {
    Preconditions.checkNotNull(other);
    return new TraceEquality(equal && other.equal, ListUtils.union(differences, other.differences));
  }

  public static TraceEquality notEqual(String reason) {
    return new TraceEquality(false, List.of(reason));
  }

  public static TraceEquality equal() {
    return new TraceEquality(true, Collections.EMPTY_LIST);
  }


}
