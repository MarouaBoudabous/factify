/**
    Copyright (C) 2016, Genome Institute of Singapore, A*STAR  

    This program is free software: you can redistribute it and/or modify
    it under the terms of the GNU General Public License as published by
    the Free Software Foundation, either version 3 of the License, or
    (at your option) any later version.

    This program is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU General Public License for more details.

    You should have received a copy of the GNU General Public License
    along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package pdfStructure;

import java.text.Normalizer;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;

import utility.Span;
import at.knowcenter.code.api.pdf.Block;
import at.knowcenter.code.api.pdf.BlockLabel;
import at.knowcenter.code.pdf.AnnotatedDocumentBuilder.LineCollector;
import at.knowcenter.code.pdf.PdfExtractionPipeline;
import at.knowcenter.code.pdf.blockclassification.BlockLabeling;

public class PDFtoStructure {

	public List<Paragraph> convert(List<Paragraph> blocks) {
		List<Paragraph> paragraphs = new ArrayList<Paragraph>();
		for(Paragraph old : blocks) {//assume all lines are adjusted aligned
			if(old.label != BlockLabel.Main) {
				paragraphs.add(old);
				continue;
			}

			List<Block> old_lines = old.t.lines;
			ArrayList<Block> new_para = new ArrayList<Block>();

			for(int i = 0; i < old_lines.size(); i++) {
				Block cur_line = old_lines.get(i);
				boolean isCurLastLineOfPara = false; 
				{//check if current line is the last line of a paragraph
					if(i == old_lines.size() - 1) isCurLastLineOfPara = true;
					else if(new_para.size() == 0) {
						isCurLastLineOfPara = false;
					}else {
						Block pre_line = new_para.get(new_para.size() - 1);
						boolean same_page = true;
						if(pre_line.getPage() != cur_line.getPage()) same_page = false;
						if(!same_page) {//based on the next line
							Block next_line = old_lines.get(i + 1);
							if(next_line.getPage() != cur_line.getPage()) isCurLastLineOfPara = true;
							else if(cur_line.getBoundingBox().maxx < next_line.getBoundingBox().minx) isCurLastLineOfPara = true;
							else if(Math.abs(next_line.getBoundingBox().maxx - cur_line.getBoundingBox().maxx)/cur_line.getBoundingBox().getWidth() > 0.05) isCurLastLineOfPara = true;
							else isCurLastLineOfPara = false;
						}else {//same page
							boolean same_column = true; //from left to right
							if(pre_line.getBoundingBox().maxx < cur_line.getBoundingBox().minx) same_column = false;
							if(same_column) {
								double right_margin = cur_line.getBoundingBox().maxx - pre_line.getBoundingBox().maxx;//cannot use # of characters, because of right
								if(right_margin < 0 && Math.abs(right_margin) > 5 * cur_line.getBoundingBox().getWidth()/cur_line.getText().length()) {
									isCurLastLineOfPara = true;
								}else isCurLastLineOfPara = false;
							}else {
								if(i == old_lines.size() - 1) {//last line
									isCurLastLineOfPara = true;
								}else {
									Block next_line = old_lines.get(i + 1);
									if(next_line.getPage() != cur_line.getPage()) isCurLastLineOfPara = true;
									else if(cur_line.getBoundingBox().maxx < next_line.getBoundingBox().minx) isCurLastLineOfPara = true;
									else if(Math.abs(next_line.getBoundingBox().maxx - cur_line.getBoundingBox().maxx)/cur_line.getBoundingBox().getWidth() > 0.05) isCurLastLineOfPara = true;
									else isCurLastLineOfPara = false;
								}
							}
						}
					}
				}
				new_para.add(cur_line);
				if(isCurLastLineOfPara) {

					LineCollector lc = new LineCollector(old.t.callback);
					lc.lines.addAll(new_para);
					for(Block line : new_para) {
						lc.lineToBlock.put(line, old.t.lineToBlock.get(line));
						lc.lineToPage.put(line, old.t.lineToPage.get(line));
					}
					Paragraph newPara = new Paragraph(lc, old.label);
					if(new_para.size() != old_lines.size()) {
						newPara.remark += "new paragraph;";
					}
					paragraphs.add(newPara);
					new_para = new ArrayList<Block>();
				}
			}

		}
		return paragraphs;
	}

	/**
	 * old; tried to split list of blocks to multiple paragraphs
	 * @param blocks
	 * @param labeling
	 * @param pipline
	 * @return
	 */
	public List<Paragraph> convert_old(List<Block> blocks, BlockLabeling labeling,PdfExtractionPipeline pipline) {

		List<Paragraph> paragraphs = new ArrayList<Paragraph>();
		for(Block old : blocks) {//assume all lines are adjusted aligned
			if(labeling.getLabel(old) == BlockLabel.Heading) {
				String text = old.getText();
				text = Normalizer.normalize(text, Normalizer.Form.NFKC);
				Paragraph para = new Paragraph(text);
				para.label = BlockLabel.Heading;
				para.headingBlock = old;
				paragraphs.add(para);
				continue;
			}
			//now labeling.getLabel(old) == BlockLabel.Main
			List<Block> old_lines = new ArrayList<Block>();
			old_lines.addAll(old.getSubBlocks());//old.getLineBlocks(); 
			ArrayList<Block> new_para = new ArrayList<Block>();

			for(int i = 0; i < old_lines.size(); i++) {
				Block cur_line = old_lines.get(i);
				boolean isCurLastLineOfPara = false; 
				{//check if current line is the last line of a paragraph
					if(i == old_lines.size() - 1) isCurLastLineOfPara = true;
					else if(new_para.size() == 0) {
						isCurLastLineOfPara = false;
					}else {
						Block pre_line = new_para.get(new_para.size() - 1);
						boolean same_page = true;
						if(pre_line.getPage() != cur_line.getPage()) same_page = false;
						if(!same_page) {//based on the next line
							Block next_line = old_lines.get(i + 1);
							if(next_line.getPage() != cur_line.getPage()) isCurLastLineOfPara = true;
							else if(cur_line.getBoundingBox().maxx < next_line.getBoundingBox().minx) isCurLastLineOfPara = true;
							else if(Math.abs(next_line.getBoundingBox().maxx - cur_line.getBoundingBox().maxx)/cur_line.getBoundingBox().getWidth() > 0.05) isCurLastLineOfPara = true;
							else isCurLastLineOfPara = false;
						}else {//same page
							boolean same_column = true; //from left to right
							if(pre_line.getBoundingBox().maxx < cur_line.getBoundingBox().minx) same_column = false;
							if(same_column) {
								double right_margin = cur_line.getBoundingBox().maxx - pre_line.getBoundingBox().maxx;//cannot use # of characters, because of right
								if(right_margin < 0 && Math.abs(right_margin) > 5 * cur_line.getBoundingBox().getWidth()/cur_line.getText().length()) {
									isCurLastLineOfPara = true;
								}else isCurLastLineOfPara = false;
							}else {
								if(i == old_lines.size() - 1) {//last line
									isCurLastLineOfPara = true;
								}else {
									Block next_line = old_lines.get(i + 1);
									if(next_line.getPage() != cur_line.getPage()) isCurLastLineOfPara = true;
									else if(cur_line.getBoundingBox().maxx < next_line.getBoundingBox().minx) isCurLastLineOfPara = true;
									else if(Math.abs(next_line.getBoundingBox().maxx - cur_line.getBoundingBox().maxx)/cur_line.getBoundingBox().getWidth() > 0.05) isCurLastLineOfPara = true;
									else isCurLastLineOfPara = false;
								}
							}
						}
					}
				}
				new_para.add(cur_line);
				if(isCurLastLineOfPara) {
					String text = "";
					if (new_para.size()>0) {
						text = pipline.clearHyphenations(new_para);
					}
					Paragraph newPara = new Paragraph(text);
					newPara.bodySubBlocks = new ArrayList<Block>();
					newPara.bodySubBlocks.addAll(new_para);
					if(new_para.size() != old_lines.size()) {
						newPara.remark += "new paragraph;";
					}
					paragraphs.add(newPara);
					new_para = new ArrayList<Block>();
				}
			}

		}
		return paragraphs;
	}
	public List<Paragraph> convert(List<Block> blocks, BlockLabeling labeling,PdfExtractionPipeline pipline) {

		List<Paragraph> paragraphs = new ArrayList<Paragraph>();
		for(Block old : blocks) {//assume all lines are adjusted aligned
			if(labeling.getLabel(old) == BlockLabel.Heading) {
				String text = old.getText();
				text = Normalizer.normalize(text, Normalizer.Form.NFKC);
				Paragraph para = new Paragraph(text);
				para.label = BlockLabel.Heading;
				para.headingBlock = old;
				HashSet<Integer> pages = new HashSet<Integer>();
				for(Block each : old.getSubBlocks()) {
					pages.add(each.getPage().getNumber());
				}
				para.pages = new Span(Collections.min(pages), Collections.max(pages) + 1);
				paragraphs.add(para);
				continue;
			}
			//now labeling.getLabel(old) == BlockLabel.Main || else
			List<Block> old_lines = new ArrayList<Block>();
			old_lines.addAll(old.getSubBlocks());//old.getLineBlocks(); 
			ArrayList<Block> new_para = new ArrayList<Block>();
			new_para.addAll(old_lines);
			String text = "";
			if (new_para.size()>0) {
				text = pipline.clearHyphenations(new_para);
			}
			Paragraph newPara = new Paragraph(text);
			newPara.bodySubBlocks = new ArrayList<Block>();
			newPara.bodySubBlocks.addAll(new_para);
			if(new_para.size() != old_lines.size()) {
				newPara.remark += "new paragraph;";
			}
			HashSet<Integer> pages = new HashSet<Integer>();
			for(Block each : old.getSubBlocks()) {
				pages.add(each.getPage().getNumber());
			}
			newPara.pages = new Span(Collections.min(pages), Collections.max(pages) + 1);
			paragraphs.add(newPara);
		}
		return paragraphs;
	}
}
