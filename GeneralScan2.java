package hw5;
/*
 * author: Helena
 * version: 1
 * This class is used to be more adaptable to different number of available threads
 */
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.RecursiveAction;

/**
 * Step 2 from HW5
 * -- change the to-fork or not-to-fork decision to be based on the portion 
 *    of the leaves in this subtree to make it more adaptable to different 
 *    number of available threads. That's more in line with the intended 
 *    functionality of ForkJoinPool.
 *
 * @param <ElemType>   data series element
 * @param <TallyType>  reduction data type
 */
public class GeneralScan2<ElemType, TallyType> {
	public static final int DEFAULT_THREAD_THRESHOLD = 10_000;
	public GeneralScan2(List<ElemType> raw) {
		this(raw, DEFAULT_THREAD_THRESHOLD);
	}
	public GeneralScan2(List<ElemType> raw, int thread_threshold) {
		reduced = false;
		n = raw.size();
		data = raw;
		height = 0;
		while (1 << height < n)
			height++;
		if (1 << height != n)
			throw new IllegalArgumentException("data size must be power of 2 for now"); // FIXME
		interior = new ArrayList<TallyType>(n - 1);
		for (int i = 0; i < n - 1; i++)
			interior.add(init());
		pool = new ForkJoinPool();
		threshold = thread_threshold;
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

	protected static final int ROOT = 0;
	protected boolean reduced;
	protected int n; // n is size of data, n-1 is size of interior
	protected List<ElemType> data;
	protected List<TallyType> interior;
	protected int height;
	protected ForkJoinPool pool;
	protected int threshold;

	protected int size() {
		return (n - 1) + n;
	}

	protected TallyType value(int i) {
		if (i < n - 1)
			return interior.get(i);
		else
			return prepare(data.get(i - (n - 1)));
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
		return right(i) >= size();
	}

	protected int leafCount(int i) {
		if (isLeaf(i))
			return 1;
		return leafCount(left(i)) + leafCount(right(i));
	}

	protected boolean reduce(int i) {
		if (!isLeaf(i)) {
			reduce(left(i));
			reduce(right(i));
			interior.set(i, combine(value(left(i)), value(right(i))));
		}
		return true;
	}

	protected void scan(int i, TallyType tallyPrior, List<TallyType> output) {
		if (isLeaf(i)) {
			output.set(i - (n - 1), combine(tallyPrior, value(i)));
		} else {
			scan(left(i), tallyPrior, output);
			scan(right(i), combine(tallyPrior, value(left(i))), output);
		}
	}

	@SuppressWarnings("serial")
	class ComputeReduction extends RecursiveAction {
		private int i;

		public ComputeReduction(int i) {
			this.i = i;
		}

		@Override
		protected void compute() {
			if (leafCount(i) < threshold) {
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
			if (leafCount(i) < threshold) {
				scan(i, tallyPrior, output);
				return;
			}
			invokeAll(
					new ComputeScan(left(i), tallyPrior, output),
					new ComputeScan(right(i), combine(tallyPrior, value(left(i))), output));
		}
	}
}
