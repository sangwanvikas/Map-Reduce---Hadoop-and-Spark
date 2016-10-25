package pkg.DataParser;

/*
 * Enum decalred for Global counters, which is used to balance page
 * rank loss for dangling nodes.
 */
public enum DATA_PROCESSOR_COUNTER {
	  TOTAL_NODES,
	  TOTAL_DANGLING_NODES,
	  DANGLING_NODES_DELTA_SUM
	};