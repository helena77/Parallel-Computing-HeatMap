package hw5;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.RecursiveAction;

/**
 * Step 3 from HW5
 *
 * With this one:
 * 	1) we use Schwartz tight loops within threads
 * 	2) we add an accum method for Tally
 * 	3) we make TallyType required to be a mutable reference type
 * 	4) we only allocate a few of the interior nodes around the root
 * 	5) we relax the N being a power of 2 constraint
 *
 * @param <ElemType>   data series element
 * @param <TallyType>  reduction data type
 */
public class GeneralScan3<ElemType, TallyType> {
	public static final int DEFAULT_THREAD_THRESHOLD = 20;
	public GeneralScan3(List<ElemType> raw) {
		this(raw, DEFAULT_THREAD_THRESHOLD);
	}
	public GeneralScan3(List<ElemType> raw, int thread_threshold) {
		reduced = false;
		n = raw.size();
		data = raw;
		height = 0;
		while ((1<<height) < n)
			height++;
		first_data = (1<<height) - 1;
		threshold = thread_threshold;
		int m = 4 * (1 + first_data/threshold);  // FIXME - calculate this correctly
		interior = new ArrayList<TallyType>(m);
		for (int i = 0; i < m; i++)
			interior.add(init());
		pool = new ForkJoinPool();
	}

	TallyType getReduction() {
		if (!reduced) {
			pool.invoke(new ComputeReduction(ROOT));
			reduced = true;
		}
		return value(ROOT);
	}

	List<TallyType> getScan() {
		if (!reduced)
			getReduction();

		List<TallyType> output = new ArrayList<TallyType>(n);
		for (int i = 0; i < data.size(); i++)
			output.add(init());
		pool.invoke(new ComputeScan(ROOT, init(), output));
		return output;
	}

	/*
	 * These are meant to be overridden by the subclass
	 */
	protected TallyType init() {
		throw new IllegalArgumentException("must implement init()"); // e.g., new TallyType();
	}
	protected TallyType prepare(ElemType datum) {
		throw new IllegalArgumentException("must implement prepare(datum)"); // e.g., new TallyType(datum);
	}
	protected TallyType combine(TallyType left, TallyType right) {
		throw new IllegalArgumentException("must implement combine(left,right)"); // e.g., left + right;
	}
	// accum must modify the object referenced by tally
	protected void accum(TallyType tally, ElemType datum) {
		throw new IllegalArgumentException("must implement accum(tally,datum)"); // e.g., tally += datum;		
	}

	protected static final int ROOT = 0;
	protected boolean reduced;
	protected int n; // n is size of data, n-1 is size of interior
	protected List<ElemType> data;
	protected List<TallyType> interior;
	protected int height;
	protected int first_data;
	protected ForkJoinPool pool;
	protected int threshold;

	protected int size() {
		return first_data + n;
	}

	protected TallyType value(int i) {
		if (i < first_data)
			return interior.get(i);
		else
			return prepare(data.get(i - first_data));
	}
	
	protected ElemType leafValue(int i) {
		if (i < first_data || i >= size())
			throw new IllegalArgumentException("bad i " + i);
		return data.get(i - first_data);
	}

	protected int parent(int i) {
		return (i - 1) / 2;
	}

	protected int left(int i) {
		return i * 2 + 1;
	}

	protected int right(int i) {
		return left(i) + 1;
	}

	protected boolean isLeaf(int i) {
		return left(i) >= size();
	}
	
	protected boolean hasRight(int i) {
		return right(i) < size();
	}
	
	protected int firstData(int i) {
		if (isLeaf(i))
			return i < first_data ? -1 : i;
		return firstData(left(i));
	}
	
	protected int lastData(int i) {
		if (isLeaf(i))
			return i < first_data ? -1 : i;
		if (hasRight(i)) {
			int r = lastData(right(i));
			if (r != -1)
				return r;
		}
		return lastData(left(i));
	}
	
	protected int dataCount(int i) {
		return lastData(i) - firstData(i);
	}

	protected void reduce(int i) {
		int first = firstData(i), last = lastData(i);
		System.out.println("reduce(" + i + ") from " + first + " to " + last);
		TallyType tally = init();
		if (first != -1)
			for (int j = first; j <= last; j++)
				accum(tally, leafValue(j));
		interior.set(i, tally);
		//System.out.println("interior: " + interior.get(i));
	}

	protected void scan(int i, TallyType tallyPrior, List<TallyType> output) {
		int first = firstData(i), last = lastData(i);
		if (first != -1)
			for (int j = first; j <= last; j++) {
				tallyPrior = combine(tallyPrior, value(j));
				output.set(j - first_data, tallyPrior);
			}
		//System.out.println("output: " + output.get(i).toString());
	}

	@SuppressWarnings("serial")
	class ComputeReduction extends RecursiveAction {
		private int i;

		public ComputeReduction(int i) {
			this.i = i;
		}

		@Override
		protected void compute() {
			if (dataCount(i) < threshold) {
				reduce(i);
				return;
			}
			invokeAll(
					new ComputeReduction(left(i)),
					new ComputeReduction(right(i)));
			interior.set(i, combine(value(left(i)), value(right(i))));
		}
	}

	@SuppressWarnings("serial")
	class ComputeScan extends RecursiveAction {
		private int i;
		private TallyType tallyPrior;
		private List<TallyType> output;

		public ComputeScan(int i, TallyType tallyPrior, List<TallyType> output) {
			this.i = i;
			this.tallyPrior = tallyPrior;
			this.output = output;
		}

		@Override
		protected void compute() {
			if (dataCount(i) < threshold) {
				scan(i, tallyPrior, output);
				return;
			}
			invokeAll(
					new ComputeScan(left(i), tallyPrior, output),
					new ComputeScan(right(i), combine(tallyPrior, value(left(i))), output));
		}
	}
}
