package test;

import com.aliasi.sentences.MedlineSentenceModel;
import com.aliasi.util.CompactHashSet;

@SuppressWarnings({ "unchecked", "rawtypes" })
public class MedlineSentenceModelTest {

	public static void main(String[] args) {
		new MedlineSentenceModelTest().run();
	}
	private void run() {
		final MedlineSentenceModel msm = new MedlineSentenceModel();
		final String[] strings = new String[] { "abc", "def", "ghi" };
		final CompactHashSet set = new CompactHashSet();

		Thread t1 = new Thread(new Runnable() {
			@Override
			public void run() {
				try {
					msm.boundaryIndices(strings, strings, 3, 1, set);
				} catch (Throwable t) {
					if (t instanceof IllegalStateException) {
						System.out.println("\n--------------------\nBug found:\n");
						t.printStackTrace(System.out);
						System.out.println("---------------------\n");
						System.exit(0);	
					}
				}
			}
		});
		Thread t2 = new Thread(new Runnable() {
			@Override
			public void run() {
				try {
					msm.boundaryIndices(strings, strings, 3, 1, set);
				} catch (Throwable t) {
					if (t instanceof IllegalStateException) {
						System.out.println("\n--------------------\nBug found:\n");
						t.printStackTrace(System.out);
						System.out.println("---------------------\n");
						System.exit(0);	
					}
				}
			}
		});

		t1.start();
		t2.start();
		try {
			t1.join();
			t2.join();
		} catch (InterruptedException ie) {
			ie.printStackTrace();
		}
	}

}
