package hw5;
/*
 * author: Helena
 * version: 1
 * This class is used to use ForkJoinPool pattern which is most closely following our 
 * recursive approach in week 5. It starts by porting that to Java and replacing the 
 * async call in C++ and the associated right branch with calls to ForkJoinPool
 */
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.RecursiveAction;

/**
 * Step 1 from HW5
 *
 * @param <ElemType>   data series element
 * @param <TallyType>  reduction data type
 */
public class GeneralScan1<ElemType, TallyType> {
	private static final int N_THREADS = 16; // fork a thread for top levels

	public GeneralScan1(List<ElemType> raw) {
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
	}

	TallyType getReduction() {
		if (!reduced) {
			ForkJoinPool pool = new ForkJoinPool(N_THREADS);
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
		scan(ROOT, init(), output);
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
			if (!isLeaf(i)) {
				if (i < N_THREADS - 2) {
					invokeAll(new ComputeReduction(left(i)), new ComputeReduction(right(i)));
				} else {
					reduce(i);
				}
				interior.set(i, combine(value(left(i)), value(right(i))));
			}
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
			if (isLeaf(i)) {
				output.set(i - (n - 1), combine(tallyPrior, value(i)));
			} else {
				if (i < N_THREADS - 2) {
					invokeAll(
							new ComputeScan(left(i), tallyPrior, output), 
							new ComputeScan(right(i), combine(tallyPrior, value(left(i))), output));
				} else {
					scan(i, tallyPrior, output);
				}
			}
		}
	}
}
