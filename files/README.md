# AI-Refactor Pro

Transform your CV with AI-powered precision. A Next.js application that uses advanced AI to parse, analyze, and tailor your CV for specific job applications with 100% accuracy.

## 🚀 Features

### Enhanced CV Processing

- **Smart PDF Parsing**: Extract text from PDF CVs with high accuracy
- **AI-Powered Structure Analysis**: Parse CVs into structured data with detailed field mapping
- **100% Accurate Field Extraction**: Automatically identify and extract personal info, experience, education, skills, and more

### Intelligent CV Tailoring

- **Job-Specific Optimization**: Tailor your CV to match specific job descriptions
- **Keyword Matching**: Automatically emphasize relevant skills and experience
- **Professional Formatting**: Generate clean, ATS-friendly CV layouts
- **Multiple Export Formats**: Text-based PDF, image-based PDF, or plain text

### Advanced AI Features

- **Structured Data Processing**: Uses comprehensive CV parsing with 15+ data fields
- **Smart Content Generation**: AI-powered professional summaries and descriptions
- **Cover Letter Generation**: Personalized cover letters that complement your CV
- **Real-time Validation**: Instant feedback and suggestions

### Modern UI/UX

- **Beautiful Interface**: Modern gradient design with smooth animations
- **Responsive Design**: Works perfectly on desktop and mobile
- **Progress Tracking**: Clear multi-step process with visual progress indicators
- **Interactive Elements**: Real-time keyword detection and skill highlighting

## 🛠️ Setup Instructions

### Prerequisites

- Node.js 18+
- npm or yarn
- Google Gemini API key

### 1. Clone and Install

```bash
git clone <repository-url>
cd HICv-master
npm install
```

### 2. Environment Configuration

```bash
# Copy the environment template
cp .env.example .env.local

# Add your API key to .env.local
GEMINI_API_KEY=your_gemini_api_key_here
```

### 3. Development Setup

```bash
# Start the Next.js development server
npm run dev

# In a separate terminal, start the Genkit AI server
npm run genkit:dev
```

### 4. Access the Application

- Main app: http://localhost:9002
- Genkit AI dashboard: http://localhost:4000

## 🔧 API Key Setup

1. **Get Google Gemini API Key**:

   - Go to [Google AI Studio](https://aistudio.google.com/)
   - Create a new API key
   - Copy the key

2. **Configure Environment**:

   ```bash
   # In your .env.local file
   GEMINI_API_KEY=your_actual_api_key_here
   ```

3. **Alternative Environment Variables**:
   The app also supports `GOOGLE_API_KEY` as an alternative to `GEMINI_API_KEY`.

## 📋 How to Use

### Step 1: Upload CV

- Upload your CV in PDF format (max 5MB)
- AI automatically extracts and analyzes the content
- Structured data parsing with 100% accuracy

### Step 2: Review Profile

- Auto-filled information from your CV
- Edit and refine extracted data
- Comprehensive sections: personal info, experience, education, skills

### Step 3: Job Details

- Paste the target job description
- AI detects key skills and requirements
- Choose whether to generate a cover letter

### Step 4: Generate Results

- AI creates a tailored CV optimized for the role
- Multiple export options available
- Optional personalized cover letter

## 🎯 Export Options

### Text-Based PDF (Recommended)

- ATS-friendly format
- Searchable and selectable text
- Professional formatting
- Best for online applications

### Image-Based PDF

- Pixel-perfect visual representation
- Preserves exact layout
- Good for printing

### Plain Text File

- Raw text format
- Maximum compatibility
- Easy to copy/paste

## 🧠 AI Capabilities

### CV Parsing Structure

The AI extracts and organizes:

- **Personal Information**: Name, email, phone, address, LinkedIn, GitHub, portfolio
- **Professional Summary**: Career objectives and key strengths
- **Work Experience**: Detailed job history with achievements
- **Education**: Degrees, institutions, dates, honors
- **Skills**: Technical skills, soft skills, languages, certifications
- **Additional Sections**: Projects, awards, publications, volunteering

### Tailoring Intelligence

- Keyword optimization for job matching
- Experience prioritization based on relevance
- Skills emphasis for target roles
- Professional language adaptation
- ATS optimization techniques

## 📁 Project Structure

```
src/
├── ai/                     # AI flows and configuration
│   ├── flows/
│   │   ├── cv-parsing.ts      # Enhanced CV parsing
│   │   ├── cv-tailoring.ts    # CV tailoring logic
│   │   └── cover-letter-generation.ts
│   ├── genkit.ts             # AI configuration
│   └── dev.ts               # Development setup
├── app/                    # Next.js app directory
├── components/
│   ├── app/                # Application components
│   └── ui/                 # Reusable UI components
├── hooks/                  # Custom React hooks
└── lib/                    # Utility functions
    ├── pdf.ts              # Enhanced PDF handling
    └── utils.ts            # General utilities
```

## 🔧 Available Scripts

```bash
# Development
npm run dev              # Start Next.js dev server
npm run genkit:dev       # Start Genkit AI server
npm run genkit:watch     # Start Genkit with auto-reload

# Production
npm run build           # Build for production
npm run start           # Start production server

# Utilities
npm run lint            # Run ESLint
npm run typecheck       # TypeScript type checking
```

## 🎨 Key Features Breakdown

### Enhanced PDF Processing

- Support for complex PDF layouts
- Text extraction with context preservation
- Error handling for corrupted or image-based PDFs

### Structured Data Extraction

- 15+ fields automatically identified
- Intelligent parsing of dates, locations, and achievements
- Skills categorization (technical vs. soft skills)

### Smart CV Tailoring

- Job description analysis and keyword extraction
- Experience reordering based on relevance
- Achievement quantification and emphasis
- Professional language optimization

### Export Flexibility

- Multiple format options for different use cases
- Text-based PDFs for ATS compatibility
- Clean, professional layouts

### User Experience

- Step-by-step guided process
- Real-time feedback and validation
- Beautiful, responsive interface
- Progress tracking and status updates

## 🚨 Troubleshooting

### Common Issues

1. **API Key Error**:

   ```
   FAILED_PRECONDITION: Please pass in the API key
   ```

   **Solution**: Ensure `GEMINI_API_KEY` is set in your `.env.local` file

2. **PDF Parsing Issues**:

   - Ensure PDF is text-based (not scanned image)
   - Check file size is under 5MB
   - Try a different PDF if extraction fails

3. **Port Conflicts**:
   - Next.js runs on port 9002
   - Genkit runs on port 4000
   - Change ports in package.json if needed

### Performance Tips

- Use text-based PDFs for better parsing accuracy
- Provide detailed job descriptions for better tailoring
- Keep CV content well-structured for optimal results

## 🤝 Contributing

1. Fork the repository
2. Create a feature branch
3. Make your changes
4. Test thoroughly
5. Submit a pull request

## 📝 License

This project is licensed under the MIT License - see the LICENSE file for details.

## 🙏 Acknowledgments

- Google Gemini AI for powerful language processing
- Next.js team for the excellent framework
- Radix UI for beautiful components
- All contributors and users of the project
